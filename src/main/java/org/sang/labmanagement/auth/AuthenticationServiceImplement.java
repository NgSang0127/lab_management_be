package org.sang.labmanagement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.auth.request.ChangePasswordRequest;
import org.sang.labmanagement.auth.request.ForgotPasswordRequest;
import org.sang.labmanagement.auth.request.ResetPasswordRequest;
import org.sang.labmanagement.auth.request.UpdateInformationUser;
import org.sang.labmanagement.auth.request.VerificationRequest;
import org.sang.labmanagement.auth.response.AuthenticationResponse;
import org.sang.labmanagement.auth.request.LoginRequest;
import org.sang.labmanagement.auth.request.RegistrationRequest;
import org.sang.labmanagement.security.email.EmailService;
import org.sang.labmanagement.security.email.EmailTemplateName;
import org.sang.labmanagement.security.email.EmailVerificationCode;
import org.sang.labmanagement.security.email.EmailVerificationRepository;
import org.sang.labmanagement.security.jwt.JwtService;
import org.sang.labmanagement.security.token.Token;
import org.sang.labmanagement.security.token.TokenRepository;
import org.sang.labmanagement.security.token.TokenType;
import org.sang.labmanagement.tfa.TwoFactorAuthenticationService;
import org.sang.labmanagement.user.Role;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImplement implements AuthenticationService {

	@Value("${application.mailing.backend.activation-url}")
	private String activationUrl;

	@Value("${application.mailing.backend.reset-password-url}")
	private String resetPasswordUrl;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final TokenRepository tokenRepo;
	private final EmailVerificationRepository emailCodeRepository;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final TwoFactorAuthenticationService twoFactorAuthenticationService;

	@Override
	public AuthenticationResponse register(RegistrationRequest request) throws MessagingException {
		if (userRepository.findByUsername(request.getUsername()).isPresent()) {
			throw new IllegalStateException("Username is already used");
		}
		User savedUser = User.builder()
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.email(request.getEmail())
				.username(request.getUsername())
				.phoneNumber(request.getPhoneNumber())
				.password(passwordEncoder.encode(request.getPassword()))
				.role(Role.STUDENT)
				.twoFactorEnabled(false)
				.secret(null)
				.accountLocked(false)
				.enabled(false)
				.build();

		userRepository.save(savedUser);
		sendValidationEmail(savedUser);
		return AuthenticationResponse.builder()
				.message("Register successful")
				.role(savedUser.getRole())
				.build();
	}

	@Override
	public AuthenticationResponse login(LoginRequest request) {
		var auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						request.getUsername(),
						request.getPassword()
				)
		);
		var user = ((User) auth.getPrincipal());
		//enable tfa
		if (user.isTwoFactorEnabled()) {
			String secret = user.getSecret();
			if (secret == null) {
				secret = twoFactorAuthenticationService.generateNewSecret();
				user.setSecret(secret);
				userRepository.save(user);
			}

			// Trả về thông tin mã QR URI và yêu cầu người dùng nhập OTP
			return AuthenticationResponse.builder()
					.message("Please enter the OTP code after scanning the QR code.")
					.tfaEnabled(true)
					.build();
		}

		var claims = new HashMap<String, Object>();
		claims.put("fullName", user.getFullName());//more add field into jwt
		var jwtToken = jwtService.generateToken(claims, user);
		var refreshToken = jwtService.generateRefreshToken(user);
		revokeAllUserTokens(user);
		saveUserToken(user, jwtToken);

		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.tfaEnabled(false)
				.refreshToken(refreshToken)
				.build();
	}
	@Override
	public void saveUserToken(User user, String jwtToken) {
		Optional<Token> existingToken = tokenRepo.findByToken(jwtToken);
		if (existingToken.isPresent()) {
			throw new IllegalStateException("Duplicate token detected. This token already exists.");
		}
		var token = Token.builder()
				.user(user)
				.token(jwtToken)
				.tokenType(TokenType.BEARER)
				.expired(false)
				.revoked(false)
				.build();
		tokenRepo.save(token);
	}

	@Override
	public void revokeAllUserTokens(User user) {
		var validUserTokens=tokenRepo.findAllValidTokenByUser(user.getId());
		if(validUserTokens.isEmpty()) {
			return;
		}
		validUserTokens.forEach(token->{
			token.setExpired(true);
			token.setRevoked(true);
		});
		tokenRepo.saveAll(validUserTokens);
	}

	@Override
	@Transactional
	public void activateAccount(String code) throws MessagingException {
		// Tìm mã xác thực trong DB
		EmailVerificationCode savedEmailVerificationCode = emailCodeRepository.findByCode(code)
				.orElseThrow(() -> new RuntimeException("The code is incorrect or has expired"));

		// Kiểm tra xem mã đã được sử dụng chưa
		if (savedEmailVerificationCode.isValidated()) {
			throw new RuntimeException("The activation code has already been used.");
		}

		// Kiểm tra xem mã đã hết hạn chưa
		if (savedEmailVerificationCode.isExpired()) {
			// Xóa mã cũ và gửi mã mới
			emailCodeRepository.delete(savedEmailVerificationCode);
			sendValidationEmail(savedEmailVerificationCode.getUser());
			throw new RuntimeException("The activation code has expired. A new code has been sent to your email.");
		}

		// Kích hoạt tài khoản người dùng
		User user = userRepository.findById(savedEmailVerificationCode.getUser().getId())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		if (user.isEnabled()) {
			throw new RuntimeException("This account is already activated.");
		}

		user.setEnabled(true);
		userRepository.save(user);

		// Đánh dấu mã là đã sử dụng
		savedEmailVerificationCode.validate();
		emailCodeRepository.save(savedEmailVerificationCode);
	}




	@Override
	public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		final String refreshToken;
		final String username;

		// Kiểm tra xem header có hợp lệ không
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			new ObjectMapper().writeValue(response.getOutputStream(),
					Map.of("error", "User not authenticated"));
			return;
		}

		// Trích xuất refreshToken từ header
		refreshToken = authHeader.substring(7);
		username = jwtService.extractUsername(refreshToken);

		// Kiểm tra xem username có tồn tại không
		if (username == null) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			new ObjectMapper().writeValue(response.getOutputStream(),
					Map.of("error", "Username not found"));
			return;
		}

		var user = userRepository.findByUsername(username).orElseThrow(
				() -> new UsernameNotFoundException("User not found")
		);

		// Kiểm tra refreshToken có hợp lệ không
		if (jwtService.isTokenValid(refreshToken, user)) {
			// Tạo accessToken mới và lưu vào database
			var accessToken = jwtService.generateToken(user);
			revokeAllUserTokens(user);
			saveUserToken(user, accessToken);

			// Tạo phản hồi chứa accessToken và refreshToken
			var authResponse = AuthenticationResponse.builder()
					.accessToken(accessToken)
					.refreshToken(refreshToken)
					.build();

			response.setStatus(HttpStatus.OK.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
		} else {
			response.setStatus(HttpStatus.FORBIDDEN.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			new ObjectMapper().writeValue(response.getOutputStream(),
					Map.of("error", "Invalid refresh token"));
		}
	}





	@Override
	public void sendValidationEmail(User user) throws MessagingException {
		var newCode = emailService.generateAndSaveActivationCode(user);
		//sendEmail
		emailService.sendEmail(
				user.getEmail(),
				user.getFullName(),
				EmailTemplateName.ACTIVATE_ACCOUNT,
				activationUrl,
				newCode,
				"Account Activation"
		);
	}



	@Override
	public User findUser(Authentication connectedUser) {
		return (User) connectedUser.getPrincipal();
	}

	@Override
	public boolean changePassword(ChangePasswordRequest request, Authentication connectedUser) {
		User user=(User) connectedUser.getPrincipal();
		if(user == null){
			return false;
		}
		if(!passwordEncoder.matches(request.getCurrentPassword(),user.getPassword())){
			return false;
		}
		if(request.getNewPassword().length() < 8){
			return false;
		}
		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
		return true;
	}

	@Override
	@Transactional
	public String forgotPassword(ForgotPasswordRequest request) throws MessagingException {
		Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
		if (userOpt.isEmpty()) {
			return "Email does not exist";
		}

		User user = userOpt.get();
		String resetPasswordCode = emailService.generateAndSaveActivationCode(user);

		try {
			emailService.sendOTPToEmail(
					user.getEmail(),
					user.getFullName(),
					EmailTemplateName.RESET_PASSWORD,
					resetPasswordCode,
					"Reset Your Password"
			);
		} catch (MessagingException e) {
			return "Failed to send email. Please try again later.";
		}

		return "A reset password code has been sent to your email!";
	}

	@Override
	public String validateResetCode(ResetPasswordRequest request) throws MessagingException {
		EmailVerificationCode resetCode=emailCodeRepository.findByCode(request.getCode()).orElse(null);
		if (resetCode == null) {
			return "Reset code invalid";
		}

		if (resetCode.isExpired()) {
			return "Expired Reset Code";
		}

		if (resetCode.isValidated()) {
			return "Reset Code has already been used";
		}
		resetCode.validate();
		emailCodeRepository.save(resetCode);

		return "Reset code is valid";
	}

	@Override
	@Transactional
	public String resetPassword(ResetPasswordRequest request) throws MessagingException {
		EmailVerificationCode resetCode=emailCodeRepository.findByCode(request.getCode()).orElse(null);
		if(resetCode == null || resetCode.isExpired()){
			return  "Invalid or expired reset code";
		}
		User user=resetCode.getUser();
		String encodedPassword = passwordEncoder.encode(request.getNewPassword());
		user.setPassword(encodedPassword);
		userRepository.save(user);

		resetCode.validate();
		emailCodeRepository.save(resetCode);

		return "Password reset successfully";
	}

	@Override
	@Transactional
	public boolean updateInformationUser(UpdateInformationUser request, Authentication connectedUser) {
		User user=(User) connectedUser.getPrincipal();
		if(user !=null){
			user.setFirstName(request.getFirstName());
			user.setLastName(request.getLastName());
			user.setEmail(request.getEmail());
			user.setImage(request.getImage());
			user.setPhoneNumber(request.getPhoneNumber());
			user.setUsername(request.getUsername());
			userRepository.save(user);
			return true;
		}
		return false;

	}

	@Override
	public AuthenticationResponse toggleTwoFactorAuthentication(Authentication connectedUser) {
		User user=(User)connectedUser.getPrincipal();
			if (user.isTwoFactorEnabled()) {

				user.setTwoFactorEnabled(false);
				user.setSecret(null);
				userRepository.save(user);
				return AuthenticationResponse.builder()
						.message("Two-Factor Authentication disabled")
						.build();
			} else {

				String secret = twoFactorAuthenticationService.generateNewSecret();
				user.setTwoFactorEnabled(true);
				user.setSecret(secret);
				userRepository.save(user);
				String qrCodeUri = twoFactorAuthenticationService.generateQrCodeImageUri(secret);
				return AuthenticationResponse.builder()
						.message("Two-Factor Authentication enabled")
						.secretImageUri(qrCodeUri)
						.build();
			}
	}

	@Override
	@Transactional
	public AuthenticationResponse verifyOtpQR(String otpCode, String username) {
		User user=userRepository.findByUsername(username)
				.orElseThrow(()-> new UsernameNotFoundException("Username not found with "+username));
		if (user != null && user.isTwoFactorEnabled()) {
			boolean isValidOtp = twoFactorAuthenticationService.isOtpValid(user.getSecret(), otpCode);
			if (isValidOtp) {
				// Nếu OTP hợp lệ, tạo và trả về token
				var claims = new HashMap<String, Object>();
				claims.put("fullName", user.getFullName());
				var jwtToken = jwtService.generateToken(claims, user);
				var refreshToken = jwtService.generateRefreshToken(user);

				revokeAllUserTokens(user);
				saveUserToken(user, jwtToken);

				return AuthenticationResponse.builder()
						.accessToken(jwtToken)
						.tfaEnabled(user.isTwoFactorEnabled())
						.refreshToken(refreshToken)
						.build();
			} else {
				throw new RuntimeException("Invalid OTP code.");
			}
		}
		throw new IllegalStateException("User not found or 2FA not enabled.");
	}

	@Override
	@Transactional
	public String verifyOtpByMail(String username) {
		User user=userRepository.findByUsername(username)
				.orElseThrow(()-> new UsernameNotFoundException("Username not found with "+username));
		String otpCode = emailService.generateAndSaveActivationCode(user);

		try {
			emailService.sendOTPToEmail(
					user.getEmail(),
					user.getFullName(),
					EmailTemplateName.TWO_FACTOR_AUTHENTICATION,
					otpCode,
					"TWO FACTOR AUTHENTICATION"
			);
		} catch (MessagingException e) {
			return "Failed to send email. Please try again later.";
		}

		return "A two factor authentication code has been sent to your email!";
	}

	@Override
	@Transactional
	public AuthenticationResponse verifyTFAEmail(VerificationRequest request) {
		User user = userRepository.findByUsername(request.getUsername())
				.orElseThrow(() -> new UsernameNotFoundException("Username not found with " + request.getUsername()));

		EmailVerificationCode otp = emailCodeRepository.findByCode(request.getCode()).orElse(null);
		if (otp == null) {
			throw new IllegalArgumentException("Invalid OTP TFA");
		}

		if (otp.isExpired()) {
			throw new IllegalArgumentException("Expired OTP TFA");
		}

		if (otp.isValidated()) {
			throw new IllegalArgumentException("OTP TFA has already been used");
		}

		otp.validate();
		emailCodeRepository.save(otp);

		var claims = new HashMap<String, Object>();
		claims.put("fullName", user.getFullName());
		var jwtToken = jwtService.generateToken(claims, user);
		var refreshToken = jwtService.generateRefreshToken(user);

		revokeAllUserTokens(user);
		saveUserToken(user, jwtToken);

		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.tfaEnabled(user.isTwoFactorEnabled())
				.refreshToken(refreshToken)
				.build();
	}

}
