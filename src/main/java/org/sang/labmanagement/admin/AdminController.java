package org.sang.labmanagement.admin;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.http.ResponseEntity;
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
			@RequestParam(name = "role", required = false) String role
	){
		return ResponseEntity.ok(adminService.findUsers(page,size,keyword,role));
	}

	@PostMapping("/users")
	public ResponseEntity<String>createUser(
			@RequestBody CreateUserByAdminRequest request
	){
		return ResponseEntity.ok().body(adminService.createUser(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<User> updateUser(
			@PathVariable Long id,
			@RequestBody CreateUserByAdminRequest request
	){
		return ResponseEntity.ok(adminService.updateUser(id,request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteUser(@PathVariable Long id) {
		return ResponseEntity.ok().body(adminService.deleteUser(id));
	}

}
