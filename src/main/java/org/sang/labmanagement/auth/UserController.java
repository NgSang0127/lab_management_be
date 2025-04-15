package org.sang.labmanagement.auth;

import jakarta.mail.MessagingException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sang.labmanagement.auth.request.ChangePasswordRequest;
import org.sang.labmanagement.auth.request.UpdateInformationUser;
import org.sang.labmanagement.tfa.TwoFactorAuthenticationService;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.utils.LogExecution;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@Slf4j
public class UserController {

	private static final String PASSWORD_MISMATCH_ERROR = "Confirm password not same new password";
	private static final String PASSWORD_CHANGE_SUCCESS = "Password is already changed!";
	private static final String PASSWORD_CHANGE_FAILURE = "Current password wrong";

	private final AuthenticationService authService;
	private final TwoFactorAuthenticationService twoFactorAuthenticationService;

	@GetMapping("/profile")
	@LogExecution
	public ResponseEntity<User> findUser(Authentication authenticatedUser) {
		log.info("Fetching profile for user: {}", authenticatedUser.getName());
		User user = getCachedUser(authenticatedUser);
		log.info("Returning profile for user: {}", user != null ? user.getUsername() : "null");
		return ResponseEntity.of(Optional.ofNullable(user));
	}

	@Cacheable(value = "userProfile", key = "#authenticatedUser.principal.username")
	public User getCachedUser(Authentication authenticatedUser) {
		return authService.findUser(authenticatedUser);
	}

	@PostMapping("/change-password")
	@CacheEvict(value = "userProfile", key = "#authenticatedUser.principal.username")
	public ResponseEntity<String> changePassword(
			@RequestBody ChangePasswordRequest request,
			Authentication authenticatedUser) {
		log.info("Changing password for user: {}", authenticatedUser.getName());
		if (!isPasswordValid(request)) {
			log.warn("Password mismatch for user: {}", authenticatedUser.getName());
			return ResponseEntity.badRequest().body(PASSWORD_MISMATCH_ERROR);
		}

		boolean isPasswordChanged = authService.changePassword(request, authenticatedUser);
		if (isPasswordChanged) {
			log.info("Password changed successfully for user: {}", authenticatedUser.getName());
			return ResponseEntity.ok(PASSWORD_CHANGE_SUCCESS);
		} else {
			log.warn("Current password wrong for user: {}", authenticatedUser.getName());
			return ResponseEntity.badRequest().body(PASSWORD_CHANGE_FAILURE);
		}
	}

	private boolean isPasswordValid(ChangePasswordRequest request) {
		return request.getNewPassword().equals(request.getConfirmPassword());
	}

	@PutMapping("/update")
	@CacheEvict(value = "userProfile", key = "#connectedUser.principal.username")
	public ResponseEntity<String> updateInformationUser(
			@RequestBody UpdateInformationUser request,
			Authentication connectedUser
	) {
		log.info("Updating information for user: {}", connectedUser.getName());
		boolean isUpdated = authService.updateInformationUser(request, connectedUser);
		if (isUpdated) {
			log.info("User information updated successfully for user: {}", connectedUser.getName());
			return ResponseEntity.ok("Update information user successful");
		} else {
			log.warn("User information update failed for user: {}", connectedUser.getName());
			return ResponseEntity.badRequest().body("Update information failed");
		}
	}

	@PostMapping("/toggle-tfa")
	@CacheEvict(value = {"userProfile", "tfaQrCode"}, key = "#connectedUser.principal.username")
	public ResponseEntity<?> toggleTwoFactorAuthentication(
			Authentication connectedUser
	) {
		log.info("Toggling 2FA for user: {}", connectedUser.getName());
		Object result = authService.toggleTwoFactorAuthentication(connectedUser);
		log.info("2FA toggled successfully for user: {}", connectedUser.getName());
		return ResponseEntity.ok(result);
	}

	@GetMapping("/tfa-qr")
	public ResponseEntity<?> getTfaQrCode(Authentication connectedUser) {
		log.info("Fetching TFA QR code for user: {}", connectedUser.getName());
		User user = (User) connectedUser.getPrincipal();
		if (!user.isTwoFactorEnabled() || user.getSecret() == null) {
			log.warn("2FA not enabled for user: {}", connectedUser.getName());
			return ResponseEntity.badRequest().body(Map.of("message", "2FA is not enabled"));
		}

		String qrCodeUri = twoFactorAuthenticationService.generateQrCodeImageUri(user.getSecret());
		log.info("Returning TFA QR code URI for user: {}", connectedUser.getName());
		return ResponseEntity.ok(Map.of("secretImageUri", qrCodeUri));
	}
}