package org.sang.labmanagement.activity;

import java.time.Instant;
import java.time.LocalDate;
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

	private final UserActivityLogService userActivityLogService;

	/**
	 * Kết thúc phiên hoạt động của người dùng.
	 *
	 * @param connectedUser Authentication đối tượng của người dùng
	 * @return Thông báo thành công
	 */
	@PostMapping("/end-session")
	public ResponseEntity<String> endSession(Authentication connectedUser) {
		userActivityLogService.endSession(connectedUser);
		return ResponseEntity.ok("Session ended successfully");
	}

	/**
	 * Lấy tổng thời gian sử dụng của người dùng trong một ngày cụ thể.
	 *
	 * @param connectedUser Authentication đối tượng của người dùng
	 * @param date           Ngày cần tính tổng thời gian sử dụng (dd/MM/yyyy)
	 * @return Tổng thời gian sử dụng (giây)
	 */
	@GetMapping("/usage-time")
	public ResponseEntity<Long> getUsageTime(
			Authentication connectedUser,
			@RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate date) {
		Long totalTime = userActivityLogService.getTotalUsageTime(connectedUser, date);
		return ResponseEntity.ok(totalTime != null ? totalTime : 0L);
	}

	/**
	 * Lấy thống kê tổng thời gian sử dụng cho các người dùng trong một ngày và theo vai trò.
	 *
	 * @param date Ngày cần thống kê (dd/MM/yyyy)
	 * @param role Vai trò của người dùng (có thể null)
	 * @param page Trang hiện tại
	 * @param size Kích thước trang
	 * @return Trang chứa danh sách UserTotalUsageDTO
	 */
	@GetMapping("/list-time")
	public ResponseEntity<PageResponse<UserTotalUsageDTO>> getTotalUsageTimeForUsersAcrossDays(
			@RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate date,
			@RequestParam(name = "role", required = false) String role,
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size) {
		PageResponse<UserTotalUsageDTO> response = userActivityLogService.getTotalUsageTimeForUsersAcrossDays(date, role, page, size);
		return ResponseEntity.ok(response);
	}

	/**
	 * Ghi lại một sự kiện hoạt động của người dùng.
	 *
	 * @param connectedUser Authentication đối tượng của người dùng
	 * @param eventType      Loại sự kiện
	 * @param timestamp      Thời gian sự kiện xảy ra (ISO8601 format)
	 * @return Thông báo thành công
	 */
	@PostMapping("/events")
	public ResponseEntity<String> logEvent(
			Authentication connectedUser,
			@RequestParam String eventType,
			@RequestParam String timestamp) {
		Instant eventTimestamp = Instant.parse(timestamp);
		userActivityLogService.logEvent(connectedUser, eventType, eventTimestamp);
		return ResponseEntity.ok("Event logged successfully");
	}
}
