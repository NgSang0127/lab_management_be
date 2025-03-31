package org.sang.labmanagement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
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
import org.sang.labmanagement.exception.AccountAlreadyActivatedException;
import org.sang.labmanagement.exception.EmailCodeException;
import org.sang.labmanagement.exception.QRCodeException;
import org.sang.labmanagement.exception.TokenException;
import org.sang.labmanagement.redis.BaseRedisServiceImpl;
import org.sang.labmanagement.security.email.EmailService;
import org.sang.labmanagement.security.email.EmailTemplateName;
import org.sang.labmanagement.security.jwt.JwtService;
import org.sang.labmanagement.security.token.TokenService;
import org.sang.labmanagement.tfa.TwoFactorAuthenticationService;
import org.sang.labmanagement.user.Role;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
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

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final TwoFactorAuthenticationService twoFactorAuthenticationService;
	private final TokenService tokenService;
	private final BaseRedisServiceImpl<String> redisService;
	private final CookieService cookieService;
	private final MessageSource messageSource;


	@Override
	public AuthenticationResponse register(RegistrationRequest request) throws MessagingException {
		Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
		Locale locale= LocaleContextHolder.getLocale();

		if (existingUser.isPresent()) {
			User user = existingUser.get();

			if (!user.isEnabled()) {
				sendValidationEmail(user.getEmail(),locale);
				return AuthenticationResponse.builder()
						.message(messageSource.getMessage("register.email.exists",null,locale))
						.role(user.getRole())
						.build();
			}
			throw new IllegalStateException(messageSource.getMessage("register.email.used", null, locale));
		}

		if (userRepository.findByUsername(request.getUsername()).isPresent()) {
			throw new IllegalStateException(messageSource.getMessage("register.username.used", null, locale));
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
		sendValidationEmail(savedUser.getEmail(),locale);

		return AuthenticationResponse.builder()
				.message(messageSource.getMessage("register.success", null, locale))
				.role(savedUser.getRole())
				.build();
	}


	@Override
	public AuthenticationResponse login(LoginRequest request, HttpServletResponse response) {
		Locale locale = LocaleContextHolder.getLocale();
		var auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						request.getUsername(),
						request.getPassword()
				)
		);
		var user = ((User) auth.getPrincipal());

		if (user.isAccountLocked()) {
			throw new LockedException(messageSource.getMessage("login.account.locked", null, locale));
		}

		// Nếu bật TFA, chưa trả về cookie vội
		if (user.isTwoFactorEnabled()) {
			String secret = user.getSecret();
			if (secret == null) {
				secret = twoFactorAuthenticationService.generateNewSecret();
				user.setSecret(secret);
				userRepository.save(user);
			}

			return AuthenticationResponse.builder()
					.message(messageSource.getMessage("login.tfa.required", null, locale))
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
				.message(messageSource.getMessage("login.success", null, locale))
				.tfaEnabled(false)
				.build();
	}




	@Override
	@Transactional
	public void activateAccount(String code,String email) throws MessagingException {
		Locale locale = LocaleContextHolder.getLocale();
		String storedCode=emailService.getEmailCode(email);

		if (storedCode == null) {
			throw new EmailCodeException(messageSource.getMessage("activate.code.incorrect",null,locale));
		}

		// Kiểm tra xem mã đã hết hạn chưa
		if (emailService.isEmailCodeExpired(email)) {
			emailService.revokeEmailCode(email);
			sendValidationEmail(email,locale);
			throw new EmailCodeException(messageSource.getMessage("activate.code.expired",null,locale));
		}


		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException(messageSource.getMessage("user.notfound",
						new Object[]{email},
						locale)));

		if (user.isEnabled()) {
			throw new AccountAlreadyActivatedException(messageSource.getMessage("activate.already",null,locale));
		}

		user.setEnabled(true);
		userRepository.save(user);

		// Đánh dấu mã là đã sử dụng-> xóa
		emailService.deleteEmailCode(email);
	}



	@Override
	public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Locale locale = LocaleContextHolder.getLocale();
		String username;
		String refreshToken = cookieService.getCookieValue(request, "refresh_token");
		if (refreshToken == null) {
			throw new TokenException(messageSource.getMessage("token.missing",null,locale));
		}
		username = jwtService.extractUsername(refreshToken);

		if (username == null) {
			throw new UsernameNotFoundException(messageSource.getMessage("user.notfound.jwt",null,locale)+ refreshToken);
		}

		var user = userRepository.findByUsername(username).orElseThrow(
				() -> new UsernameNotFoundException(messageSource.getMessage("user.notfound.username",new Object[]{username}
						,locale))
		);


		// Kiểm tra xem Refresh Token có bị thu hồi không
		if (redisService.get("blacklist:refresh:" + refreshToken) != null) {
			throw new TokenException(messageSource.getMessage("token.revoked",null,locale));
		}


		String storedRefreshToken = tokenService.getRefreshToken(username);
		if (!tokenService.isRefreshTokenValid(username,storedRefreshToken)) {
			throw new TokenException(messageSource.getMessage("token.invalid",null,locale));
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
			throw new TokenException(messageSource.getMessage("token.invalid",null,locale));
		}
	}


	@Override
	public void sendValidationEmail(String email,Locale locale) throws MessagingException {
		var newCode = emailService.generateAndSaveActivationCode(email);
		emailService.sendEmail(
				email,
				EmailTemplateName.ACTIVATE_ACCOUNT,
				activationUrl,
				newCode,
				"Account Activation",
				locale
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
		Locale locale = LocaleContextHolder.getLocale();
		Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
		if (userOpt.isEmpty()) {
			return messageSource.getMessage("forgot.email.not.exist",null,locale);
		}

		User user = userOpt.get();
		String resetPasswordCode = emailService.generateAndSaveActivationCode(request.getEmail());

		try {
			emailService.sendOTPToEmail(
					user.getEmail(),
					user.getFullName(),
					EmailTemplateName.RESET_PASSWORD,
					resetPasswordCode,
					"Reset Your Password",
					locale
			);
		} catch (MessagingException e) {
			return messageSource.getMessage("forgot.email.failed",null,locale);
		}

		return messageSource.getMessage("forgot.email.sent",null,locale);
	}

	@Override
	public String validateResetCode(ResetPasswordRequest request) throws MessagingException {
		Locale locale = LocaleContextHolder.getLocale();
		String storedResetCode = emailService.getEmailCode(request.getEmail());

		if (storedResetCode == null) {
			return messageSource.getMessage("reset.code.invalid", null, locale);
		}

		if (emailService.isEmailCodeExpired(request.getEmail())) {
			emailService.deleteEmailCode(request.getEmail());
			return messageSource.getMessage("reset.code.expired", null, locale);
		}

		emailService.revokeEmailCode(request.getEmail());
		return messageSource.getMessage("reset.code.valid", null, locale);
	}

	@Override
	@Transactional
	public String resetPassword(ResetPasswordRequest request) throws MessagingException {
		Locale locale = LocaleContextHolder.getLocale();
		String email = request.getEmail();
		String storedResetCode = emailService.getEmailCode(email);

		if (storedResetCode == null || !emailService.isEmailCodeMatch(email, request.getCode())) {
			return messageSource.getMessage("reset.code.invalid_or_expired", null, locale);
		}

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException(
						messageSource.getMessage("user.notfound", new Object[]{email}, locale)
				));

		String encodedPassword = passwordEncoder.encode(request.getNewPassword());
		user.setPassword(encodedPassword);
		userRepository.save(user);
		emailService.deleteEmailCode(email);

		return messageSource.getMessage("password.reset.success", null, locale);
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
		Locale locale = LocaleContextHolder.getLocale();
		User user = (User) connectedUser.getPrincipal();

		if (user.isTwoFactorEnabled()) {
			user.setTwoFactorEnabled(false);
			user.setSecret(null);
			userRepository.save(user);
			return AuthenticationResponse.builder()
					.message(messageSource.getMessage("tfa.disabled", null, locale))
					.build();
		} else {
			String secret = twoFactorAuthenticationService.generateNewSecret();
			user.setTwoFactorEnabled(true);
			user.setSecret(secret);
			userRepository.save(user);
			String qrCodeUri = twoFactorAuthenticationService.generateQrCodeImageUri(secret);
			return AuthenticationResponse.builder()
					.message(messageSource.getMessage("tfa.enabled", null, locale))
					.secretImageUri(qrCodeUri)
					.build();
		}
	}

	@Override
	@Transactional
	public AuthenticationResponse verifyOtpQR(String otpCode, String username,HttpServletResponse response) {
		Locale locale=LocaleContextHolder.getLocale();
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException(messageSource.getMessage("user.notfound",
						new Object[]{username},locale)));

		if (!user.isTwoFactorEnabled()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, messageSource.getMessage("2fa.not.enabled", null, locale));
		}

		boolean isValidOtp = twoFactorAuthenticationService.isOtpValid(user.getSecret(), otpCode);
		if (!isValidOtp) {
			throw new QRCodeException(messageSource.getMessage("otp.qr.invalid", null, locale));
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
		Locale locale=LocaleContextHolder.getLocale();
		System.out.println("Locale: " + locale);
		String errorMessage = messageSource.getMessage("otp.email.sent", null, locale);
		System.out.println("Error message: " + errorMessage);
		User user=userRepository.findByUsername(username)
				.orElseThrow(()-> new UsernameNotFoundException(messageSource.getMessage("user.notfound", new Object[]{username}, locale)));
		String otpCode = emailService.generateAndSaveActivationCode(user.getEmail());

		try {
			emailService.sendOTPToEmail(
					user.getEmail(),
					user.getFullName(),
					EmailTemplateName.TWO_FACTOR_AUTHENTICATION,
					otpCode,
					"TWO FACTOR AUTHENTICATION",
					locale
			);
		} catch (MessagingException e) {
			return messageSource.getMessage("email.send.failed", null, locale);
		}

		return messageSource.getMessage("otp.email.sent", null, locale);
	}
	@Override
	@Transactional
	public AuthenticationResponse verifyTFAEmail(VerificationRequest request, HttpServletResponse response) {
		Locale locale = LocaleContextHolder.getLocale();
		User user = userRepository.findByUsername(request.getUsername())
				.orElseThrow(() -> new UsernameNotFoundException(messageSource.getMessage("user.notfound", new Object[]{request.getUsername()}, locale)));

		String storedOtp = emailService.getEmailCode(user.getEmail());

		if (storedOtp == null) {
			throw new EmailCodeException(messageSource.getMessage("otp.email.invalid", null, locale));
		}

		if (emailService.isEmailCodeExpired(user.getEmail())) {
			throw new EmailCodeException(messageSource.getMessage("otp.email.expired", null, locale));
		}

		if (!emailService.isEmailCodeMatch(user.getEmail(), request.getCode())) {
			throw new EmailCodeException(messageSource.getMessage("otp.email.mismatch", null, locale));
		}

		// ✅ Chỉ xóa OTP sau khi xác thực thành công
		emailService.deleteEmailCode(user.getEmail());

		tokenService.revokeRefreshToken(user.getUsername());

		var claims = new HashMap<String, Object>();
		claims.put("fullName", user.getFullName());

		var jwtToken = jwtService.generateToken(claims, user);
		var refreshToken = jwtService.generateRefreshToken(user);

		cookieService.addCookie(response, "access_token", jwtToken, null);
		cookieService.addCookie(response, "refresh_token", refreshToken, null);

		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.tfaEnabled(user.isTwoFactorEnabled())
				.refreshToken(refreshToken)
				.build();
	}



}
