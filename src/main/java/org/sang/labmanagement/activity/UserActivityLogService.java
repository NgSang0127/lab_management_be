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
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${application.activity.inactivity-timeout-minutes}")
	private long inactivityTimeoutMinutes;

	private final UserActivityLogRepository userActivityLogRepository;
	private final UserRepository userRepository;
	private final UserActivityEventRepository userActivityEventRepository;

	// Bắt đầu một phiên hoạt động mới cho người dùng nếu chưa có phiên nào đang hoạt động.
	@Transactional
	public void startSession(String username) {
		User user = getUserByUsername(username);
		LocalDate today = LocalDate.now();
		LocalDateTime now = LocalDateTime.now();

		// Tìm các phiên đang hoạt động
		List<UserActivityLog> ongoingSessions = userActivityLogRepository.findOngoingSessions(user.getUsername());

		// Đóng các phiên đang hoạt động từ ngày trước
		for (UserActivityLog session : ongoingSessions) {
			if (!session.getDate().equals(today)) {
				session.setEndTime(now);
				session.setDuration(Duration.between(session.getStartTime(), now).getSeconds());
			}
		}
		userActivityLogRepository.saveAll(ongoingSessions);

		// Kiểm tra xem có phiên đang hoạt động trong ngày hiện tại không
		boolean hasOngoingSessionToday = ongoingSessions.stream()
				.anyMatch(log -> log.getDate().equals(today) && log.getEndTime() == null);

		// Nếu không có phiên trong ngày hiện tại, tạo phiên mới
		if (!hasOngoingSessionToday) {
			UserActivityLog newSession = UserActivityLog.builder()
					.user(user)
					.startTime(now)
					.date(today)
					.lastActivityTime(now)
					.build();
			userActivityLogRepository.save(newSession);
		}
	}

	// Cập nhật lastActivityTime cho các phiên đang hoạt động
	@Transactional
	public void updateLastActivityTime(String username) {
		List<UserActivityLog> ongoingSessions = userActivityLogRepository.findOngoingSessions(username);
		LocalDateTime now = LocalDateTime.now();
		ongoingSessions.forEach(session -> session.setLastActivityTime(now));
		userActivityLogRepository.saveAll(ongoingSessions);
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
	@CacheEvict(value = "usageTime", allEntries = true)
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
				startOfDay, endOfDay, roleParam != null ? Role.valueOf(roleParam) : null, pageable);

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
	 * Dọn dẹp các phiên không kết thúc sau 30 phút không hoạt động.
	 */
	@Transactional
	@CacheEvict(value = "usageTime", allEntries = true)
	public void cleanupSessions() {
		LocalDateTime now = LocalDateTime.now();
		// Ngưỡng không hoạt động: 30 phút
		LocalDateTime inactivityThreshold = now.minusMinutes(inactivityTimeoutMinutes);

		Pageable pageable = PageRequest.of(0, 100); // Xử lý 100 phiên mỗi lần
		Page<UserActivityLog> inactiveSessionsPage;
		do {
			inactiveSessionsPage = userActivityLogRepository.findByEndTimeIsNull(pageable);
			List<UserActivityLog> inactiveSessions = inactiveSessionsPage.getContent().stream()
					.filter(session -> session.getLastActivityTime() != null
							&& session.getLastActivityTime().isBefore(inactivityThreshold))
					.toList();
			for (UserActivityLog session : inactiveSessions) {
				session.setEndTime(now);
				session.setDuration(Duration.between(session.getStartTime(), now).getSeconds());
			}
			userActivityLogRepository.saveAll(inactiveSessions);
			pageable = inactiveSessionsPage.nextPageable();
		} while (inactiveSessionsPage.hasNext());
	}

	/**
	 * Lên lịch dọn dẹp các phiên không hoạt động mỗi 5 phút.
	 */
	@Scheduled(fixedRate = 300000) // Chạy mỗi 5 phút (300000ms)
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

