package org.sang.labmanagement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.auth.response.AuthenticationResponse;
import org.sang.labmanagement.auth.request.LoginRequest;
import org.sang.labmanagement.auth.request.RegistrationRequest;
import org.sang.labmanagement.exception.OperationNotPermittedException;
import org.sang.labmanagement.security.email.EmailService;
import org.sang.labmanagement.security.email.EmailTemplateName;
import org.sang.labmanagement.security.email.EmailVerificationCode;
import org.sang.labmanagement.security.email.EmailVerificationRepository;
import org.sang.labmanagement.security.jwt.JwtService;
import org.sang.labmanagement.security.token.Token;
import org.sang.labmanagement.security.token.TokenRepository;
import org.sang.labmanagement.security.token.TokenType;
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

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImplement implements AuthenticationService {

	@Value("${application.mailing.backend.activation-url}")
	private String activationUrl;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final TokenRepository tokenRepo;
	private final EmailVerificationRepository emailCodeRepository;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

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
				.accountLocked(false)
				.enabled(false)
				.build();
		if (request.getUsername().startsWith("ITIT")) {
			savedUser.setRole(Role.STUDENT);
		} else if (request.getUsername().startsWith("IU")) {
			savedUser.setRole(Role.TEACHER);
		}
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
		var claims = new HashMap<String, Object>();
		var user = ((User) auth.getPrincipal());
		claims.put("fullName", user.getFullName());//more add field into jwt
		var jwtToken = jwtService.generateToken(claims, user);
		var refreshToken = jwtService.generateRefreshToken(user);
		revokeAllUserTokens(user);
		saveUserToken(user, jwtToken);

		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.refreshToken(refreshToken)
				.build();
	}

	@Override
	public void activateAccount(String code) throws MessagingException {
		// Find the email verification code
		EmailVerificationCode savedEmailVerificationCode =
				emailCodeRepository.findByCode(code)
						.orElseThrow(() -> new RuntimeException("The code is not correct or has expired"));

		// Check if the code has expired
		if (LocalDateTime.now().isAfter(savedEmailVerificationCode.getExpiresAt())) {
			// Resend the activation email
			sendValidationEmail(savedEmailVerificationCode.getUser());
			throw new RuntimeException("The activation code has expired. A new code has been sent to your email.");
		}

		// Activate the user's account
		var user = userRepository.findById(savedEmailVerificationCode.getUser().getId())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		if (user.isEnabled()) {
			throw new RuntimeException("This account is already activated.");
		}

		user.setEnabled(true);
		userRepository.save(user);

		// Mark the code as validated
		savedEmailVerificationCode.setValidatedAt(LocalDateTime.now());
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
	public void saveUserToken(User user, String jwtToken) {
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
	public User findUser(Authentication connectedUser) {
		return (User) connectedUser.getPrincipal();
	}
}
