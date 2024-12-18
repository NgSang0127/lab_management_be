package org.sang.labmanagement.auth;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.auth.request.ForgotPasswordRequest;
import org.sang.labmanagement.auth.request.LoginRequest;
import org.sang.labmanagement.auth.request.RegistrationRequest;
import org.sang.labmanagement.auth.request.ResetPasswordRequest;
import org.sang.labmanagement.auth.response.AuthenticationResponse;
import org.sang.labmanagement.user.UserDetailsServiceImplement;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final UserRepository userRepository;
	@Value("${application.mailing.success-url}")
	private  String successUrl;
	@Value("${application.mailing.error-url}")
	private String errorUrl;
	
	private final AuthenticationService authService;
	private final UserDetailsServiceImplement userDetailsServiceImplement;


	@PostMapping("/register")
	public ResponseEntity<AuthenticationResponse> register(
			@Valid @RequestBody RegistrationRequest request
	) throws MessagingException {
		return ResponseEntity.ok(authService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthenticationResponse> login(
			@Valid @RequestBody LoginRequest request
	) {
		return ResponseEntity.ok(authService.login(request));
	}


	@GetMapping("/activate-account")
	public void activateAccount(
			@RequestParam("code") String code,
			HttpServletResponse response) throws IOException {
		try {
			authService.activateAccount(code);
			// Redirect to the frontend success page
			response.sendRedirect(successUrl);
		} catch (RuntimeException | MessagingException e) {
			// Redirect to an error page or handle the error accordingly
			response.sendRedirect(errorUrl + URLEncoder.encode(e.getMessage(), "UTF-8"));
		}
	}

	@PostMapping("/refresh-token")
	public void refreshToken(
			HttpServletRequest request,
			HttpServletResponse response
	) throws IOException {
		System.out.println("Refresh token endpoint accessed");
		authService.refreshToken(request, response);

	}

	@PostMapping("/forgot-password")
	public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) throws MessagingException {
		try {
			String result = authService.forgotPassword(request);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	@PostMapping("/validate-reset-code")
	public ResponseEntity<String> validateResetCode(@RequestBody ResetPasswordRequest request) {
		try {
			String result = authService.validateResetCode(request);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			// Trả về BadRequest nếu có lỗi trong quá trình xử lý
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping("/reset-password")
	public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) throws MessagingException {
		try {
			String result = authService.resetPassword(request);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			// Trả về BadRequest nếu có lỗi trong quá trình xử lý
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}



}
