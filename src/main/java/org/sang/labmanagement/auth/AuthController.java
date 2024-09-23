package org.sang.labmanagement.auth;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.auth.request.LoginRequest;
import org.sang.labmanagement.auth.request.RegistrationRequest;
import org.sang.labmanagement.auth.response.AuthenticationResponse;
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
@RequestMapping("/auth")
public class AuthController {

	@Value("${application.mailing.success-url}")
	private  String successUrl;
	@Value("${application.mailing.error-url}")
	private String errorUrl;
	
	private final AuthenticationService authService;

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
	public ResponseEntity<AuthenticationResponse>  refreshToken(
			HttpServletRequest request,
			HttpServletResponse response
			//lấy ra Header :authorization
	) throws IOException {
		return ResponseEntity.ok(authService.refreshToken(request, response));

	}


}
