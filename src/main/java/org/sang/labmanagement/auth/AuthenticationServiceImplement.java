package org.sang.labmanagement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
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
import org.sang.labmanagement.cookie.CookieService;
import org.sang.labmanagement.redis.BaseRedisServiceImpl;
import org.sang.labmanagement.security.email.EmailService;
import org.sang.labmanagement.security.email.EmailTemplateName;
import org.sang.labmanagement.security.email.EmailVerificationCode;
import org.sang.labmanagement.security.email.EmailVerificationRepository;
import org.sang.labmanagement.security.jwt.JwtService;
import org.sang.labmanagement.security.token.TokenService;
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
import org.springframework.web.server.ResponseStatusException;

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
	private final EmailVerificationRepository emailCodeRepository;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final TwoFactorAuthenticationService twoFactorAuthenticationService;
	private final TokenService tokenService;
	private final BaseRedisServiceImpl<String> redisService;
	private final CookieService cookieService;

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
	public AuthenticationResponse login(LoginRequest request, HttpServletResponse response) {
		var auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						request.getUsername(),
						request.getPassword()
				)
		);
		var user = ((User) auth.getPrincipal());

		// Nếu bật TFA, chưa trả về cookie vội
		if (user.isTwoFactorEnabled()) {
			String secret = user.getSecret();
			if (secret == null) {
				secret = twoFactorAuthenticationService.generateNewSecret();
				user.setSecret(secret);
				userRepository.save(user);
			}

			return AuthenticationResponse.builder()
					.message("Please enter the OTP code after scanning the QR code.")
					.tfaEnabled(true)
					.build();
		}

		// Nếu không bật TFA, tạo token và lưu vào cookie luôn
		var claims = new HashMap<String, Object>();
		claims.put("fullName", user.getFullName());
		var jwtToken = jwtService.generateToken(claims, user);
		var refreshToken = jwtService.generateRefreshToken(user);

		// Set cookie
		cookieService.addCookie(response, "access_token", jwtToken, null);
		cookieService.addCookie(response, "refresh_token", refreshToken, null);

		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.tfaEnabled(false)
				.build();
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
		String username;
		String refreshToken = cookieService.getCookieValue(request, "refresh_token");
		username = jwtService.extractUsername(refreshToken);

		if (username == null) {
			sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Username not found");
			return;
		}

		var user = userRepository.findByUsername(username).orElseThrow(
				() -> new UsernameNotFoundException("User not found")
		);

		// Kiểm tra xem Refresh Token có bị thu hồi không
		if (redisService.get("blacklist:refresh:" + refreshToken) != null) {
			sendErrorResponse(response, HttpStatus.FORBIDDEN, "Refresh token is revoked");
			return;
		}


		String storedRefreshToken = tokenService.getRefreshToken(username);
		if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
			sendErrorResponse(response, HttpStatus.FORBIDDEN, "Invalid or expired refresh token");
			return;
		}

		if (jwtService.isTokenValid(refreshToken, user)) {
			var accessToken = jwtService.generateToken(user);

			// Thu hồi Refresh Token cũ trước khi tạo mới
			tokenService.revokeRefreshToken(username);
			var newRefreshToken = jwtService.generateRefreshToken(user);

			// Ghi đè cookie mới
			cookieService.addCookie(response, "access_token", accessToken, null);
			cookieService.addCookie(response, "refresh_token", newRefreshToken, null);


			var authResponse = AuthenticationResponse.builder()
					.accessToken(accessToken)
					.refreshToken(newRefreshToken)
					.build();

			response.setStatus(HttpStatus.OK.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
		} else {
			sendErrorResponse(response, HttpStatus.FORBIDDEN, "Invalid refresh token");
		}
	}



	private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		new ObjectMapper().writeValue(response.getOutputStream(), Map.of("error", message));
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
	public AuthenticationResponse verifyOtpQR(String otpCode, String username,HttpServletResponse response) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Username not found: " + username));

		if (!user.isTwoFactorEnabled()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "2FA is not enabled for this user.");
		}

		boolean isValidOtp = twoFactorAuthenticationService.isOtpValid(user.getSecret(), otpCode);
		if (!isValidOtp) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP code.");
		}

		// Xóa Refresh Token cũ trước khi tạo mới
		tokenService.revokeRefreshToken(username);

		var claims = new HashMap<String, Object>();
		claims.put("fullName", user.getFullName());

		var jwtToken = jwtService.generateToken(claims, user);
		var refreshToken = jwtService.generateRefreshToken(user);

// Ghi đè cookie mới
		cookieService.addCookie(response, "access_token", jwtToken, null);
		cookieService.addCookie(response, "refresh_token", refreshToken, null);


		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.tfaEnabled(true)
				.refreshToken(refreshToken)
				.build();
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
	public AuthenticationResponse verifyTFAEmail(VerificationRequest request,HttpServletResponse response) {
		User user = userRepository.findByUsername(request.getUsername())
				.orElseThrow(() -> new UsernameNotFoundException("Username not found: " + request.getUsername()));

		EmailVerificationCode otp = emailCodeRepository.findByCode(request.getCode())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP TFA"));

		if (otp.isExpired()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expired OTP TFA");
		}

		if (otp.isValidated()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP TFA has already been used");
		}

		otp.validate();
		emailCodeRepository.save(otp);

		// Xóa Refresh Token cũ trước khi tạo mới
		tokenService.revokeRefreshToken(user.getUsername());

		var claims = new HashMap<String, Object>();
		claims.put("fullName", user.getFullName());

		var jwtToken = jwtService.generateToken(claims, user);
		var refreshToken = jwtService.generateRefreshToken(user);

		cookieService.addCookie(response, "access_token", jwtToken, null);
		cookieService.addCookie(response, "refresh_token", refreshToken,null);


		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.tfaEnabled(user.isTwoFactorEnabled())
				.refreshToken(refreshToken)
				.build();
	}


}
