package org.sang.labmanagement.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.auth.request.ForgotPasswordRequest;
import org.sang.labmanagement.auth.request.LoginRequest;
import org.sang.labmanagement.auth.request.RegistrationRequest;
import org.sang.labmanagement.auth.request.ResetPasswordRequest;
import org.sang.labmanagement.auth.request.VerificationRequest;
import org.sang.labmanagement.auth.response.AuthenticationResponse;
import org.sang.labmanagement.user.UserRepository;
import org.sang.labmanagement.utils.LogExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

	@Value("${application.mailing.success-url}")
	private  String successUrl;
	@Value("${application.mailing.error-url}")
	private String errorUrl;

	private final AuthenticationService authService;

	@Operation(summary = "Register a new user", description = "Creates a new user account and sends verification email")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Registration successful",
					content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "409", description = "User already exists")
	})
	@PostMapping("/register")
	@LogExecution
	public ResponseEntity<AuthenticationResponse> register(
			@Valid @RequestBody RegistrationRequest request
	) throws MessagingException {
		return ResponseEntity.ok(authService.register(request));
	}

	@Operation(summary = "Login user", description = "Authenticates a user and returns a JWT token")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Login successful",
					content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
			@ApiResponse(responseCode = "401", description = "Invalid credentials")
	})
	@PostMapping("/login")
	@LogExecution
	public ResponseEntity<AuthenticationResponse> login(
			@RequestBody LoginRequest request,
			HttpServletResponse response
	) {
		return ResponseEntity.ok(authService.login(request,response));
	}

	@Operation(summary = "Activate user account", description = "Activates a user account using email verification code")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "302", description = "Redirect to success/error page"),
			@ApiResponse(responseCode = "400", description = "Invalid activation code")
	})
	@GetMapping("/activate-account")
	public void activateAccount(
			@Parameter(description = "User's email address") @RequestParam("email") String email,
			@Parameter(description = "Activation code") @RequestParam("code") String code,
			HttpServletResponse response) throws IOException {
		try {
			authService.activateAccount(code,email);
			// Redirect to the frontend success page
			response.sendRedirect(successUrl);
		} catch (RuntimeException | MessagingException e) {
			// Redirect to an error page or handle the error accordingly
			response.sendRedirect(errorUrl + URLEncoder.encode(e.getMessage(), "UTF-8"));
		}
	}

	@Operation(summary = "Refresh authentication token", description = "Issues a new JWT token using refresh token")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Token refreshed successfully",
					content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
			@ApiResponse(responseCode = "401", description = "Invalid refresh token")
	})
	@PostMapping("/refresh-token")
	public ResponseEntity<AuthenticationResponse> refreshToken(
			HttpServletRequest request,
			HttpServletResponse response
	) throws IOException {
		System.out.println("Refresh token endpoint accessed");
		try {
			return ResponseEntity.ok(authService.refreshToken(request, response));
		} catch (org.sang.labmanagement.exception.TokenException e) {
			// Return a 401 with a more helpful error message but don't throw an exception
			return ResponseEntity
					.status(HttpStatus.UNAUTHORIZED)
					.body(AuthenticationResponse.builder()
							.message(e.getMessage())
							.build());
		}
	}

	@Operation(summary = "Request password reset", description = "Sends password reset email to user")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reset email sent successfully"),
			@ApiResponse(responseCode = "404", description = "User not found")
	})
	@PostMapping("/forgot-password")
	public ResponseEntity<AuthenticationResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) throws MessagingException {
			AuthenticationResponse result = authService.forgotPassword(request);
			return ResponseEntity.ok(result);

	}

	@Operation(summary = "Validate reset code", description = "Validates the password reset code")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reset code is valid"),
			@ApiResponse(responseCode = "400", description = "Invalid reset code")
	})
	@PostMapping("/validate-reset-code")
	public ResponseEntity<AuthenticationResponse> validateResetCode(@RequestBody ResetPasswordRequest request)
			throws MessagingException {
			AuthenticationResponse result = authService.validateResetCode(request);
			return ResponseEntity.ok(result);
	}

	@Operation(summary = "Reset password", description = "Resets user password using reset code")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Password reset successful"),
			@ApiResponse(responseCode = "400", description = "Invalid reset code or password")
	})
	@PostMapping("/reset-password")
	public ResponseEntity<AuthenticationResponse> resetPassword(@RequestBody ResetPasswordRequest request) throws MessagingException {
			AuthenticationResponse result = authService.resetPassword(request);
			return ResponseEntity.ok(result);
	}

	@Operation(summary = "Verify QR code", description = "Verifies two-factor authentication QR code")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "QR code verified successfully",
					content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid QR code")
	})
	@PostMapping("/verify-qr")
	public ResponseEntity<?> verifyCode(
			@RequestBody VerificationRequest request,
			HttpServletResponse response
	) {
		return ResponseEntity.ok(authService.verifyOtpQR(request.getCode(),request.getUsername(),response));
	}

	@Operation(summary = "Send email OTP", description = "Sends one-time password via email for two-factor authentication")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OTP sent successfully"),
			@ApiResponse(responseCode = "400", description = "Failed to send OTP")
	})
	@PostMapping("/email-otp")
	public ResponseEntity<?> sendTFAEmail(@RequestBody Map<String, String> request) throws MessagingException {
		String username = request.get("username");
		try {
			String result = authService.verifyOtpByMail(username);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@Operation(summary = "Verify email OTP", description = "Verifies one-time password sent via email")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OTP verified successfully",
					content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid OTP")
	})
	@PostMapping("/verify-otp")
	public ResponseEntity<AuthenticationResponse> verifyTFAEmail(
			@RequestBody VerificationRequest request,
			HttpServletResponse response
	) {
		return ResponseEntity.ok(authService.verifyTFAEmail(request,response));
	}
}