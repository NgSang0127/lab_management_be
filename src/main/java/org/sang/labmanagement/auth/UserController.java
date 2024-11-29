package org.sang.labmanagement.auth;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {
	private final AuthenticationService authService;

	@GetMapping("/profile")
	public ResponseEntity<User> findUserByJwt(Authentication connectedUser) {
		User user = authService.findUser(connectedUser);
		return ResponseEntity.ok(user);
	}

}
