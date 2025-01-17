package org.sang.labmanagement.activity;

import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.user.Role;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserActivityLogService {

	private final UserActivityLogRepository userActivityLogRepository;
	private final UserRepository userRepository;
	private final UserActivityEventRepository userActivityEventRepository;


	//Bắt đầu một phiên hoạt động mới cho người dùng nếu chưa có phiên nào đang hoạt động.
	@Transactional
	public void startSession(String username) {
		User user = getUserByUsername(username);
		LocalDate today = LocalDate.now();

		boolean hasOngoingSession = userActivityLogRepository.findByUserAndDate(user, today).stream()
				.anyMatch(log -> log.getEndTime() == null);

		if (!hasOngoingSession) {
			UserActivityLog newSession = UserActivityLog.builder()
					.user(user)
					.startTime(LocalDateTime.now())
					.date(today)
					.lastActivityTime(LocalDateTime.now())
					.build();
			userActivityLogRepository.save(newSession);
		}
	}

	/**
	 * Kết thúc tất cả các phiên hoạt động đang hoạt động của người dùng.
	 *
	 * @param connectedUser Authentication đối tượng của người dùng
	 */
	@Transactional
	@CacheEvict(value = "usageTime", allEntries = true)
	public void endSession(Authentication connectedUser) {
		User user = getUserFromAuthentication(connectedUser);
		List<UserActivityLog> ongoingSessions = userActivityLogRepository.findOngoingSessions(user.getUsername());
		LocalDateTime now = LocalDateTime.now();

		ongoingSessions.forEach(session -> {
			session.setEndTime(now);
			session.setDuration(Duration.between(session.getStartTime(), now).getSeconds());
		});

		userActivityLogRepository.saveAll(ongoingSessions);
	}

	/**
	 * Ghi lại một sự kiện hoạt động của người dùng.
	 *
	 * @param connectedUser Authentication đối tượng của người dùng
	 * @param eventType      Loại sự kiện
	 * @param timestamp      Thời gian sự kiện xảy ra
	 */
	@Transactional
	@CacheEvict(value = "usageTime", allEntries = true) // Xóa cache khi ghi sự kiện
	public void logEvent(Authentication connectedUser, String eventType, Instant timestamp) {
		User user = getUserFromAuthentication(connectedUser);
		UserActivityEvent event = UserActivityEvent.builder()
				.user(user)
				.eventType(eventType)
				.timestamp(timestamp)
				.build();
		userActivityEventRepository.save(event);

		// Cập nhật lastActivityTime trong phiên hoạt động đang hoạt động
		List<UserActivityLog> ongoingSessions = userActivityLogRepository.findOngoingSessions(user.getUsername());
		ongoingSessions.forEach(session -> session.setLastActivityTime(LocalDateTime.ofInstant(timestamp, session.getStartTime().atZone(
				ZoneId.systemDefault()).getZone())));
		userActivityLogRepository.saveAll(ongoingSessions);
	}

	/**
	 * Tính tổng thời gian sử dụng của người dùng trong một ngày cụ thể.
	 *
	 * @param connectedUser Authentication đối tượng của người dùng
	 * @param date           Ngày cần tính tổng thời gian sử dụng
	 * @return Tổng thời gian sử dụng (giây)
	 */
	@Cacheable(value = "usageTime", key = "#connectedUser.name + '-' + #date")
	public Long getTotalUsageTime(Authentication connectedUser, LocalDate date) {
		User user = getUserFromAuthentication(connectedUser);
		LocalDateTime startOfDay = date.atStartOfDay();
		LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

		Long totalDuration = userActivityLogRepository.getTotalUsageTimeByDay(user.getUsername(), startOfDay, endOfDay);
		return Optional.ofNullable(totalDuration).orElse(0L);
	}

	/**
	 * Lấy thống kê tổng thời gian sử dụng cho các người dùng trong một ngày và theo vai trò.
	 *
	 * @param date Ngày cần thống kê
	 * @param role Vai trò của người dùng (có thể null)
	 * @param page Trang hiện tại
	 * @param size Kích thước trang
	 * @return Trang chứa danh sách UserTotalUsageDTO
	 */
	public PageResponse<UserTotalUsageDTO> getTotalUsageTimeForUsersAcrossDays(
			LocalDate date, String role, int page, int size) {

		LocalDateTime startOfDay = date.atStartOfDay();
		LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

		Pageable pageable = PageRequest.of(page, size);


		String roleParam = (role != null && !role.isEmpty()) ? role.toUpperCase() : null;

		Page<UserTotalUsageDTO> userUsagePage = userActivityLogRepository.getTotalUsageTimeForUsersAcrossDays(
				startOfDay, endOfDay, Role.valueOf(roleParam), pageable);

		return PageResponse.<UserTotalUsageDTO>builder()
				.content(userUsagePage.getContent())
				.number(userUsagePage.getNumber())
				.size(userUsagePage.getSize())
				.totalElements(userUsagePage.getTotalElements())
				.totalPages(userUsagePage.getTotalPages())
				.first(userUsagePage.isFirst())
				.last(userUsagePage.isLast())
				.build();
	}

	/**
	 * Dọn dẹp các phiên không kết thúc sau 24 giờ.
	 */
	@Transactional
	public void cleanupSessions() {
		LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
		List<UserActivityLog> sessionsToCleanup = userActivityLogRepository.findSessionsToCleanup(cutoffTime);
		sessionsToCleanup.forEach(session -> {
			session.setEndTime(cutoffTime);
			session.setDuration(Duration.between(session.getStartTime(), cutoffTime).getSeconds());
		});
		userActivityLogRepository.saveAll(sessionsToCleanup);
	}


	@Scheduled(cron = "0 0 0 * * ?") // Hàng ngày vào lúc 00:00
	@Transactional
	public void scheduledCleanupSessions() {
		cleanupSessions();
	}

	// Helper methods

	private User getUserFromAuthentication(Authentication authentication) {
		return (User) authentication.getPrincipal();
	}

	private User getUserByUsername(String username) {
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found with username: " + username));
	}

}

