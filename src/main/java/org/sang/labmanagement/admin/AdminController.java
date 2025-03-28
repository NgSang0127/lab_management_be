package org.sang.labmanagement.admin;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.OperationNotPermittedException;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

	private final UserRepository userRepository;
	private final AdminService adminService;

	@GetMapping("/users")
	public ResponseEntity<PageResponse<User>>getUsers(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "role", required = false) String role,
			Authentication connectedUser
	){
		return ResponseEntity.ok(adminService.findUsers(page,size,keyword,role,connectedUser));
	}

	@PostMapping("/users")
	public ResponseEntity<?>createUser(
			@RequestBody CreateUserByAdminRequest request
	){
		return ResponseEntity.ok().body(adminService.createUser(request));
	}

	@PutMapping("/users/{id}")
	public ResponseEntity<?> updateUser(
			@PathVariable Long id,
			@RequestBody CreateUserByAdminRequest request,
			@AuthenticationPrincipal User currentUser // Lấy user đang thực hiện request từ Security Context
	) {
		User targetUser = userRepository.findById(id)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		if (!adminService.canModifyUser(currentUser.getRole(), targetUser.getRole())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can't have permission to modify this user");
		}

		return ResponseEntity.ok(adminService.updateUser(id, request));
	}


	@DeleteMapping("/users/{id}")
	public ResponseEntity<?> deleteUser(@PathVariable Long id,Authentication connectedUser) {
		return ResponseEntity.ok().body(adminService.deleteUser(id,connectedUser));
	}

}
