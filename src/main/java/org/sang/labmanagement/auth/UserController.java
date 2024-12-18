package org.sang.labmanagement.auth;

import jakarta.mail.MessagingException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.auth.request.ChangePasswordRequest;
import org.sang.labmanagement.auth.request.UpdateInformationUser;
import org.sang.labmanagement.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {

	private static final String PASSWORD_MISMATCH_ERROR = "Confirm password not same new password";
	private static final String PASSWORD_CHANGE_SUCCESS = "Password is already changed!";
	private static final String PASSWORD_CHANGE_FAILURE = "Current password wrong";

	private final AuthenticationService authService;

	@GetMapping("/profile")
	public ResponseEntity<User> findUserByJwt(Authentication authenticatedUser) {
		return ResponseEntity.of(Optional.ofNullable(authService.findUser(authenticatedUser)));
	}

	@PostMapping("/change-password")
	public ResponseEntity<String> changePassword(
			@RequestBody ChangePasswordRequest request,
			Authentication authenticatedUser) {

		if (!isPasswordValid(request)) {
			return ResponseEntity.badRequest().body(PASSWORD_MISMATCH_ERROR);
		}

		boolean isPasswordChanged = authService.changePassword(request, authenticatedUser);
		return isPasswordChanged
				? ResponseEntity.ok(PASSWORD_CHANGE_SUCCESS)
				: ResponseEntity.badRequest().body(PASSWORD_CHANGE_FAILURE);
	}

	// Private helper method for password validation
	private boolean isPasswordValid(ChangePasswordRequest request) {
		return request.getNewPassword().equals(request.getConfirmPassword());
	}

	@PutMapping("/update")
	public ResponseEntity<String> updateInformationUser(
			@RequestBody UpdateInformationUser request,
			Authentication connectedUser
	){
		boolean isUpdated=authService.updateInformationUser(request,connectedUser);
		if(isUpdated){
			return ResponseEntity.ok("Update information user successful");
		}else{
			return ResponseEntity.badRequest().body("Update information failed");
		}
	}
}