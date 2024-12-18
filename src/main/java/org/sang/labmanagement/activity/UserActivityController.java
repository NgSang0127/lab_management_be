package org.sang.labmanagement.activity;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-activity")
public class UserActivityController {

	private final UserActivityLogService userActivityService;

	@PostMapping("/end-session")
	public ResponseEntity<String> endSession(Authentication connectedUser) {
		userActivityService.endSession(connectedUser);
		return ResponseEntity.ok("Session ended successfully");
	}

	@GetMapping("/usage-time")
	public ResponseEntity<Long> getUsageTime(
			Authentication connectedUser,
			@RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate date) {
		Long totalTime = userActivityService.getTotalUsageTime(connectedUser, date);
		return ResponseEntity.ok(totalTime != null ? totalTime : 0L);
	}

	@GetMapping("/list-time")
	public ResponseEntity<PageResponse<UserTotalUsageDTO>>getTotalUsageTimeForUsersAcrossDays(
			@RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate date,
			@RequestParam (name = "role") String role,
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
	){
		return ResponseEntity.ok(userActivityService.getTotalUsageTimeForUsersAcrossDays(date,role,page,size));
	}
}
