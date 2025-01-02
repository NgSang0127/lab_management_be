package org.sang.labmanagement.activity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.user.Role;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserActivityLogService {

	private final UserActivityLogRepository userActivityLogRepository;
	private final UserRepository userRepository;
	private final UserActivityEventRepository userActivityEventRepository;


	// Start a new session for the user
	public void startSession(String username) {
		User user = getUserByUsername(username);
		LocalDate today = LocalDate.now();

		// Check if no session exists today
		if (userActivityRepository.findByUserAndDate(user, today).stream()
				.noneMatch(log -> log.getEndTime() == null)) {
			UserActivityLog newSession = UserActivityLog.builder()
					.user(user)
					.startTime(LocalDateTime.now())
					.date(today)
					.build();
			userActivityRepository.save(newSession);
		}
	}

	// End any ongoing session for the authenticated user
	public void endSession(Authentication connectedUser) {
		User user = getUserFromAuthentication(connectedUser);
		List<UserActivityLog> ongoingSessions = userActivityRepository.findOngoingSessions(user.getUsername());
		for (UserActivityLog session : ongoingSessions) {
			session.setEndTime(LocalDateTime.now());
			session.setDuration(Duration.between(session.getStartTime(), session.getEndTime()).getSeconds());
			userActivityRepository.save(session);
		}
	}

	// Calculate total usage time for a specific day
	public Long getTotalUsageTime(Authentication connectedUser, LocalDate date) {
		User user = (User) connectedUser.getPrincipal();
		List<UserActivityLog> logs = userActivityRepository.findByUserAndDate(user, date);

		long totalDuration = 0;

		for (UserActivityLog log : logs) {
			// Nếu phiên chưa kết thúc, tính thời gian cho đến hiện tại
			LocalDateTime endTime = log.getEndTime() != null ? log.getEndTime() : LocalDateTime.now();

			// Kiểm tra nếu phiên kéo dài qua nhiều ngày
			if (log.getStartTime().toLocalDate().isEqual(date) && endTime.toLocalDate().isEqual(date)) {
				// Phiên kết thúc trong ngày, tính thời gian từ startTime đến endTime
				totalDuration += Duration.between(log.getStartTime(), endTime).getSeconds();
			} else {
				// Trường hợp phiên kéo dài qua nhiều ngày

				// Tính thời gian của phần đầu tiên trong ngày (từ startTime đến 23:59:59)
				LocalDateTime endOfDay = date.atTime(23, 59, 59);
				totalDuration += Duration.between(log.getStartTime(), endOfDay).getSeconds();

				// Tính thời gian của phần còn lại vào ngày hôm sau
				LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();
				if (endTime.isAfter(startOfNextDay)) {
					totalDuration += Duration.between(startOfNextDay, endTime).getSeconds();
				}
			}
		}
		return totalDuration;
	}


	public PageResponse<UserTotalUsageDTO> getTotalUsageTimeForUsersAcrossDays(
			LocalDate date, String role, int page, int size) {

		LocalDateTime startOfDay = date.atStartOfDay();
		LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

		// Lấy logs từ repository
		List<Object[]> logs = userActivityRepository.getUserActivityLogsForTimeRange(startOfDay, endOfDay,
				Role.valueOf(role.toUpperCase()));

		// Gộp dữ liệu theo userId
		Map<Long, UserTotalUsageDTO> userTimeMap = new HashMap<>();
		for (Object[] log : logs) {
			User user = (User) log[0];
			LocalDateTime startTime = (LocalDateTime) log[1];
			LocalDateTime endTime = (LocalDateTime) log[2];

			long durationInSecond = 0;
			if (startTime != null && endTime != null) {
				// Xử lý thời gian chồng lấn ngày
				if (startTime.isBefore(startOfDay) && endTime.isAfter(endOfDay)) {
					durationInSecond = Duration.between(startOfDay, endOfDay).getSeconds();
				} else if (startTime.isBefore(startOfDay)) {
					durationInSecond = Duration.between(startOfDay, endTime).getSeconds();
				} else if (endTime.isAfter(endOfDay)) {
					durationInSecond = Duration.between(startTime, endOfDay).getSeconds();
				} else {
					durationInSecond = Duration.between(startTime, endTime).getSeconds();
				}
			}

			// Gộp thời gian sử dụng cho user
			userTimeMap.merge(
					user.getId(),
					new UserTotalUsageDTO(user, durationInSecond),
					(existing, newEntry) -> {
						existing.setTotalUsageTime(existing.getTotalUsageTime() + newEntry.getTotalUsageTime());
						return existing;
					}
			);
		}

		// Chuyển Map thành danh sách và sắp xếp theo thời gian sử dụng giảm dần
		List<UserTotalUsageDTO> userTotalUsageList = new ArrayList<>(userTimeMap.values());

		// Nếu danh sách trống, tạo một phần tử mặc định
		if (userTotalUsageList.isEmpty()) {
			userTotalUsageList.add(new UserTotalUsageDTO(User.builder().username("Default User").build(), 0L)); // Thêm phần tử mặc định
		}

		userTotalUsageList.sort(Comparator.comparingLong(UserTotalUsageDTO::getTotalUsageTime).reversed());

		// Phân trang thủ công
		int start = page * size;
		int end = Math.min(start + size, userTotalUsageList.size());
		List<UserTotalUsageDTO> pagedContent = userTotalUsageList.subList(start, end);
		for (UserTotalUsageDTO hehe:pagedContent
			 ) {
			System.out.println(hehe
			);
		}

		// Trả về response
		return PageResponse.<UserTotalUsageDTO>builder()
				.content(pagedContent)
				.number(page)
				.size(size)
				.totalElements(userTotalUsageList.size())
				.totalPages((int) Math.ceil((double) userTotalUsageList.size() / size))
				.first(page == 0)
				.last(end == userTotalUsageList.size())
				.build();
	}



	private User getUserFromAuthentication(Authentication connectedUser) {
		return (User) connectedUser.getPrincipal();
	}

	private User getUserByUsername(String username) {
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found with username: " + username));
	}


}

