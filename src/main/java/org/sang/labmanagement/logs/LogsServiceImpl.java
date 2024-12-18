package org.sang.labmanagement.logs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.course.Course;
import org.sang.labmanagement.course.CourseRepository;
import org.sang.labmanagement.logs.dto.CourseLogStatistics;
import org.sang.labmanagement.logs.dto.DailyLogStatistics;
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
public class LogsServiceImpl implements LogsService {

	private final UserRepository userRepository;
	private final LogsRepository logRepository;
	private final CourseRepository courseRepository;

	@Override
	public void saveLog(String endpoint, LocalDateTime timestamp, String action, Authentication connectedUser,
			String codeCourse, String NH, String TH, String timetableName, String ipAddress, String userAgent){
		// Ensure the principal is of type User
		if (connectedUser.getPrincipal() instanceof User) {
			User user = (User) connectedUser.getPrincipal();

			Course course = null;
			if (codeCourse != null) {
				// Only attempt to find the course if courseId is not null
				course = courseRepository.findByCodeAndNHAndTH(codeCourse,NH,TH).orElse(null);
			} else {
				// Handle case where courseId is null (if necessary)
				// You could log or throw an exception here
			}

			// Create the log entry
			Logs logs = Logs.builder()
					.endpoint(endpoint)
					.timestamp(timestamp)
					.action(action)
					.user(user)
					.course(course)
					.ipAddress(ipAddress)
					.userAgent(userAgent)
					.build();

			logRepository.save(logs);
		} else {
			// Log or throw exception if principal is not a User
			throw new IllegalStateException("Authentication principal is not a User");
		}
	}


	@Override
	public PageResponse<Logs> getLogsBetweenDates(LocalDateTime startDate, LocalDateTime endDate,int page,int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
		Page<Logs> logsPage = logRepository.findByTimestampBetween(startDate, endDate, pageable);
		return PageResponse.<Logs>builder()
				.content(logsPage.getContent())
				.number(logsPage.getNumber())
				.size(logsPage.getSize())
				.totalElements(logsPage.getTotalElements())
				.totalPages(logsPage.getTotalPages())
				.first(logsPage.isFirst())
				.last(logsPage.isLast())
				.build();
	}

	@Override
	public List<Logs> getLogsByAction(String action) {
		return logRepository.findByAction(action);
	}

	@Override
	public List<Logs> getLogsByCourse(Long courseId) {
		Optional<Course> courseOpt = courseRepository.findById(courseId);
		return courseOpt.map(logRepository::findByCourse).orElse(null);
	}

	@Override
	public List<Logs> getLogsByUser(Long userId) {
		Optional<User> userOpt = userRepository.findById(userId);
		return userOpt.map(logRepository::findByUser).orElse(null);
	}

	@Override
	public List<DailyLogStatistics> getDailyLogStatistics(LocalDate startDate, LocalDate endDate) {
		return logRepository.findLogsGroupByDate(startDate.atStartOfDay(), endDate.atTime(23,59,59));
	}

	@Override
	public List<CourseLogStatistics> getCourseLogStatistics(LocalDate startDate, LocalDate endDate) {
		return logRepository.findLogsGroupByCourse(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
	}
}
