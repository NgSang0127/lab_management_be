package org.sang.labmanagement.logs;

import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.course.Course;
import org.sang.labmanagement.course.CourseRepository;
import org.sang.labmanagement.logs.dto.CourseLogStatistics;
import org.sang.labmanagement.logs.dto.DailyLogStatistics;
import org.sang.labmanagement.logs.dto.LabUsageStatisticsDTO;
import org.sang.labmanagement.room.Room;
import org.sang.labmanagement.room.RoomRepository;
import org.sang.labmanagement.timetable.Timetable;
import org.sang.labmanagement.timetable.TimetableRepository;
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
	private final TimetableRepository timetableRepository;
	private final CourseRepository courseRepository;
	private final RoomRepository roomRepository;
	private static final int LESSONS_PER_DAY = 11;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	@Override
	public void saveLog(String endpoint, LocalDateTime timestamp, String action, Authentication connectedUser,
			String codeCourse, String NH, String TH, String timetableName,
			String ipAddress, String userAgent) {

		User user = null;
		if (connectedUser != null && connectedUser.getPrincipal() instanceof User) {
			user = (User) connectedUser.getPrincipal();
		}

		Course course = null;
		if (codeCourse != null) {
			course = courseRepository.findByCodeAndNHAndTH(codeCourse, NH, TH).orElse(null);
		}

		Logs logs = Logs.builder()
				.endpoint(endpoint)
				.timestamp(timestamp)
				.action(action)
				.user(user) // có thể null nếu anonymous
				.course(course)
				.ipAddress(ipAddress)
				.userAgent(userAgent)
				.build();

		logRepository.save(logs);
	}

	@Override
	public PageResponse<Logs> getLogsBetweenDates(LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
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
		return logRepository.findLogsGroupByDate(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
	}

	@Override
	public List<CourseLogStatistics> getCourseLogStatistics(LocalDate startDate, LocalDate endDate) {
		return logRepository.findLogsGroupByCourse(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
	}

	@Override
	@Transactional
	public List<LabUsageStatisticsDTO> getUsageStatistics(LocalDate startDate, LocalDate endDate) {
		System.out.println("Processing lab usage statistics from " + startDate + " to " + endDate);

		// Lấy tất cả phòng lab
		List<Room> rooms = roomRepository.findAll();
		Map<Long, LabUsageStatisticsDTO> statisticsMap = new HashMap<>();
		Map<Long, Integer> studentLessonsMap = new HashMap<>();

		// Khởi tạo thống kê cho mỗi phòng
		for (Room room : rooms) {
			LabUsageStatisticsDTO dto = new LabUsageStatisticsDTO();
			dto.setRoomId(room.getId());
			dto.setRoomName(room.getName());
			dto.setUsagePercentage(0.0);
			statisticsMap.put(room.getId(), dto);
			studentLessonsMap.put(room.getId(), 0);
			System.out.println("Room " + room.getName() + " - Capacity: " + room.getCapacity());
		}

		// Tính tổng số tiết khả dụng
		long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
		int totalAvailableLessons = (int) (totalDays * LESSONS_PER_DAY);
		System.out.println("Total available lessons: " + totalAvailableLessons);

		// Lấy tất cả Timetable
		List<Timetable> timetables = timetableRepository.findAllWithRoom();
		System.out.println("Found " + timetables.size() + " timetables");

		// Xử lý từng Timetable
		for (Timetable timetable : timetables) {
			System.out.println("Processing Timetable: " + timetable.getTimetableName() + ", StudyTime: " + timetable.getStudyTime());

			if (timetable.getRoom() == null || timetable.getDayOfWeek() == null ||
					timetable.getTotalLessonDay() == 0) {
				System.out.println("Skipping Timetable due to missing data: " + timetable);
				continue;
			}

			Long roomId = timetable.getRoom().getId();
			if (!statisticsMap.containsKey(roomId)) {
				System.out.println("Skipping Timetable - Room not found: " + roomId);
				continue;
			}

			// Tính số tiết mỗi ngày
			int lessonsPerDay = timetable.getTotalLessonDay();
			System.out.println("Lessons per day: " + lessonsPerDay);

			// Lấy danh sách khoảng thời gian từ studyTime
			List<LocalDate[]> studyPeriods = extractStudyPeriods(timetable.getStudyTime());
			if (studyPeriods.isEmpty()) {
				System.out.println("No valid study periods found for Timetable: " + timetable);
				continue;
			}

			// Tính tổng số ngày hợp lệ
			int totalValidDays = 0;
			for (LocalDate[] period : studyPeriods) {
				LocalDate periodStart = period[0];
				LocalDate periodEnd = period[1];
				System.out.println("Study period: " + periodStart + " to " + periodEnd);

				// Giới hạn trong startDate và endDate
				periodStart = periodStart.isAfter(startDate) ? periodStart : startDate;
				periodEnd = periodEnd.isBefore(endDate) ? periodEnd : endDate;
				if (periodStart.isAfter(periodEnd)) {
					System.out.println("Period does not intersect with request range: " + periodStart + " to " + periodEnd);
					continue;
				}

				// Lấy các ngày hợp lệ trong khoảng thời gian này
				List<LocalDate> validDates = getValidDates(periodStart, periodEnd, timetable.getDayOfWeek(),
						timetable.getCancelDates());
				totalValidDays += validDates.size();
				System.out.println("Valid days for period: " + validDates.size());
			}

			// Tính student-lessons
			int totalLessons = lessonsPerDay * totalValidDays;
			int students = timetable.getNumberOfStudents();
			int studentLessons = students * totalLessons;
			System.out.println("Timetable: " + timetable.getTimetableName() + " - Total lessons: " + totalLessons +
					", Students: " + students + ", Student-lessons: " + studentLessons);

			// Cộng dồn student-lessons cho phòng
			studentLessonsMap.put(roomId, studentLessonsMap.get(roomId) + studentLessons);
		}

		// Tính tần suất sử dụng (%) cho từng phòng
		for (Room room : rooms) {
			Long roomId = room.getId();
			LabUsageStatisticsDTO dto = statisticsMap.get(roomId);
			if (dto == null) {
				continue;
			}

			int studentLessons = studentLessonsMap.getOrDefault(roomId, 0);
			int maxStudentLessons = room.getCapacity() * totalAvailableLessons;
			if (maxStudentLessons > 0) {
				double usagePercentage = (studentLessons * 100.0) / maxStudentLessons;
				dto.setUsagePercentage(usagePercentage);
				System.out.println("Room " + room.getName() + " - Student-lessons: " + studentLessons +
						", Max Student-lessons: " + maxStudentLessons + ", Usage: " + usagePercentage + "%");
			} else {
				System.out.println("Room " + room.getName() + " - Max Student-lessons is 0, skipping calculation");
			}
		}

		return new ArrayList<>(statisticsMap.values());
	}

	private List<LocalDate[]> extractStudyPeriods(String studyTime) {
		List<LocalDate[]> periods = new ArrayList<>();

		if (studyTime == null || studyTime.trim().isEmpty()) {
			return periods;
		}

		// Tách chuỗi theo dòng mới "\n" nếu có nhiều khoảng thời gian hoặc ngày lẻ
		String[] periodStrings = studyTime.split("\n");

		for (String periodString : periodStrings) {
			String[] dates = periodString.split("-");

			if (dates.length == 2) {
				LocalDate startDate = LocalDate.parse(dates[0].trim(), DATE_FORMATTER);
				LocalDate endDate = LocalDate.parse(dates[1].trim(), DATE_FORMATTER);
				periods.add(new LocalDate[]{startDate, endDate});
			} else if (dates.length == 1) { // Trường hợp chỉ có một ngày
				LocalDate singleDate = LocalDate.parse(dates[0].trim(), DATE_FORMATTER);
				periods.add(new LocalDate[]{singleDate, singleDate});
			}
		}

		return periods;
	}

	private List<LocalDate> getValidDates(LocalDate start, LocalDate end, DayOfWeek dayOfWeek,
			Set<LocalDate> cancelDates) {
		List<LocalDate> validDates = new ArrayList<>();
		LocalDate current = start;

		while (!current.isAfter(end)) {
			if (current.getDayOfWeek() == dayOfWeek &&
					(cancelDates == null || !cancelDates.contains(current))) {
				validDates.add(current);
			}
			current = current.plusDays(1);
		}

		return validDates;
	}
}

