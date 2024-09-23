package org.sang.labmanagement.timetable;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.sang.labmanagement.course.Course;
import org.sang.labmanagement.course.CourseRepository;
import org.sang.labmanagement.room.Room;
import org.sang.labmanagement.room.RoomRepository;
import org.sang.labmanagement.room.RoomStatus;
import org.sang.labmanagement.semester.Semester;
import org.sang.labmanagement.semester.SemesterRepository;
import org.sang.labmanagement.timetable.LessonTime.LessonTime;
import org.sang.labmanagement.timetable.LessonTime.LessonTimeRepository;
import org.sang.labmanagement.user.Role;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.sang.labmanagement.user.instructor.Department;
import org.sang.labmanagement.user.instructor.Instructor;
import org.sang.labmanagement.user.instructor.InstructorRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TimetableServiceImplement implements TimetableService {

	private String lastExtractedSession = "";  // Biến lưu trạng thái lần trích xuất gần nhất
	private final List<LessonTime> lessonTimeList = new ArrayList<>();
	private String previousCode = "";  // Mã MH
	private int previousCredits = 0;   // Số TC
	private int previousNH = 0;        // NH
	private int previousTH = 0;        // TH
	private String previousClassId = ""; // Lớp
	private int previousNumberOfStudents = 0;

	private final TimetableRepository timetableRepository;
	private final RoomRepository roomRepository;
	private final InstructorRepository instructorRepository;
	private final UserRepository userRepository;
	private final CourseRepository courseRepository;
	private final SemesterRepository semesterRepository;
	private final LessonTimeRepository lessonTimeRepository;

	@Override
	public Timetable createTimetable(Timetable timetable) {
		return timetableRepository.save(timetable);
	}

	@Override
	public List<Timetable> getAllTimetableByWeek(LocalDate startDate, LocalDate endDate) {
		List<Timetable> matchingTimetables = new ArrayList<>();

		List<Timetable> allTimetables = timetableRepository.findAll(); // Lấy tất cả thời khóa biểu

		for (Timetable timetable : allTimetables) {
			List<LocalDate[]> periods = extractStudyPeriods(timetable.getStudyTime());

			// Kiểm tra từng khoảng thời gian xem nó có nằm trong khoảng startDate và endDate không
			for (LocalDate[] period : periods) {
				LocalDate periodStart = period[0];
				LocalDate periodEnd = period[1];

				if (!(periodEnd.isBefore(startDate) || periodStart.isAfter(endDate))) {
					matchingTimetables.add(timetable);
					break; // Nếu một khoảng thời gian hợp lệ, thêm vào danh sách và dừng kiểm tra tiếp
				}
			}
		}

		return matchingTimetables;
	}


	public List<LocalDate[]> extractStudyPeriods(String studyTime) {
		List<LocalDate[]> periods = new ArrayList<>();

		// Tách chuỗi theo dòng mới "\n" nếu có nhiều khoảng thời gian
		String[] periodStrings = studyTime.split("\n");

		for (String periodString : periodStrings) {
			// Tách từng khoảng thời gian theo dấu "-" để lấy ngày bắt đầu và kết thúc
			String[] dates = periodString.split("-");
			if (dates.length == 2) {
				LocalDate startDate = LocalDate.parse(dates[0].trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
				LocalDate endDate = LocalDate.parse(dates[1].trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
				periods.add(new LocalDate[]{startDate, endDate});
			}
		}

		return periods;
	}




	@Override
	public List<Timetable> importExcelData(MultipartFile file) throws Exception {

		List<Timetable> timetables = new ArrayList<>();
		Room currentRoom = null;
		Semester currentSemester = null;
		int skipRows = 0;

		try (InputStream inputStream = file.getInputStream()) {
			Workbook workbook = WorkbookFactory.create(inputStream);
			Sheet sheet = workbook.getSheetAt(0);
			for (Row row : sheet) {
				if (currentSemester == null) {
					currentSemester = extractSemesterFromExcel(row);
					if (currentSemester != null) {
						continue;
					}
				}
				if (isIrrelevantTableRow(row)) {
					saveAllLessonTimes();
				}
			}

			for (Row row : sheet) {
				System.out.println("Processing row number: " + row.getRowNum());

				// Phát hiện bảng mới dựa vào sự xuất hiện của từ "Phòng:"
				if (isRoomInfoRow(row)) {
					currentRoom = extractRoomFromExcel(row.getCell(1).getStringCellValue());
					System.out.println("New table detected. Current room: " + currentRoom.getName() + ", capacity: "
							+ currentRoom.getCapacity());
					skipRows = 4;  // Bỏ qua 4 dòng sau khi tìm thấy phòng mới
					continue;
				}

				if (isIrrelevantTableRow(row)) {
					System.out.println("Skipping irrelevant table.");
					continue;
				}

				if (skipRows > 0) {
					skipRows--;  // Giảm số dòng cần bỏ qua
					continue;
				}
				// Kiểm tra xem toàn bộ dòng có trống hay không
				boolean isRowEmpty = true;
				for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
					if (row.getCell(cellIndex) != null && !row.getCell(cellIndex).toString().trim().isEmpty()) {
						isRowEmpty = false;
						break;
					}
				}

				// Bỏ qua các dòng không chứa dữ liệu hoặc tiêu đề của bảng

				if (row.getLastCellNum() < 22) {
					System.out.println("Skipping row with insufficient columns.");
					continue;
				}

				// Lấy thông tin Mã MH, Số tín chỉ, NH, TH, Lớp, và Số sinh viên va kiem tra 2 ô merge chưa du lieu
				String code = getStringCellValue(row, 0);
				code = code.isEmpty() ? previousCode : (previousCode = code);
				int credits = (int) getNumericCellValue(row, 7);
				credits = (credits == 0) ? previousCredits : (previousCredits = credits);
				int NH = (int) getNumericCellValue(row, 8);
				NH = (NH == 0) ? previousNH : (previousNH = NH);
				int TH = (int) getNumericCellValue(row, 9);
				TH = (TH == 0) ? previousTH : (previousTH = TH);
				String classId = getStringCellValue(row, 10);
				classId = classId.isEmpty() ? previousClassId : (previousClassId = classId);
				int numberOfStudents = (int) getNumericCellValue(row, 11);
				numberOfStudents = (numberOfStudents == 0) ? previousNumberOfStudents
						: (previousNumberOfStudents = numberOfStudents);

				// Xử lý FullName giảng viên
				String fullName = getStringCellValue(row, 21).trim();
				String[] names = getFirstNameAndLastNameFromFullName(fullName);
				String firstName = names[0];
				String lastName = names[1];

				User user = User.builder()
						.firstName(firstName)
						.lastName(lastName)
						.username(getStringCellValue(row, 19)) // Mã viên chức
						.role(Role.TEACHER)
						.build();

				User savedUser = userRepository.findByUsername(user.getUsername()).orElse(null);
				if (savedUser == null) {
					savedUser = userRepository.save(user);
				}

				String instructorId = getStringCellValue(row, 19).trim();
				Instructor savedInstructor = null;
				if (!instructorId.isEmpty()) {
					savedInstructor = instructorRepository.findByInstructorId(instructorId).orElse(null);
					if (savedInstructor == null) {
						savedInstructor = Instructor.builder()
								.instructorId(instructorId) // Mã viên chức
								.user(savedUser)
								.department(Department.IT)
								.build();
						savedInstructor = instructorRepository.save(savedInstructor);
					}
				} else {
					System.out.println("Skipping course as instructor_id is empty or null.");
					continue;  // Bỏ qua nếu không có instructor
				}

				// Kiểm tra nếu mã môn học và tên môn học trùng với dòng trước
				Course savedCourse = courseRepository.findByCodeAndNHAndTH(code, NH, TH).orElse(null);
				if (savedCourse == null) {
					// Nếu không có môn học nào trùng Mã MH và các yếu tố khác, thì tạo mới
					savedCourse = Course.builder()
							.code(code)
							.name(getStringCellValue(row, 2)) // Tên môn học
							.credits(credits)
							.NH(NH)
							.timetables(new HashSet<>())
							.semester(currentSemester)
							.TH(TH)
							.instructor(savedInstructor)
							.build();
					savedCourse = courseRepository.save(savedCourse);
				}

				// Lấy thông tin DayOfWeek từ Excel
				int dayOfWeekNumber = (int) getNumericCellValue(row, 12);
				DayOfWeek dayOfWeek = convertDayToDayOfWeekFromExcel(dayOfWeekNumber);

				int startLesson = (int) getNumericCellValue(row, 13);

				// Tạo Timetable
				Timetable timetable = Timetable.builder()
						.courses(Set.of(savedCourse))
						.instructor(savedInstructor)
						.totalLessonSemester((int) getNumericCellValue(row, 5))
						.classId(classId)
						.startLesson(startLesson)
						.numberOfStudents(numberOfStudents)
						.dayOfWeek(dayOfWeek)
						.totalLessonDay((int) getNumericCellValue(row, 14))
						.room(currentRoom)
						.studyTime(getStringCellValue(row, 16))
						.build();


				LessonTime startLessonTime = lessonTimeRepository.findByLessonNumber(timetable.getStartLesson());
				if (startLessonTime == null) {
					System.out.println("No LessonTime found for Start Lesson: " + timetable.getStartLesson());
				}
				int endLessonNumber = timetable.getStartLesson() + timetable.getTotalLessonDay() - 1;
				LessonTime endLessonTime = lessonTimeRepository.findByLessonNumber(endLessonNumber);
				if (endLessonTime == null) {
					System.out.println("No LessonTime found for End Lesson: " + endLessonNumber);
				}

				timetable.setStartLessonTime(startLessonTime);
				timetable.setEndLessonTime(endLessonTime);

				timetableRepository.save(timetable);

				savedCourse.getTimetables().add(timetable);
				courseRepository.save(savedCourse);

				timetables.add(timetable);
			}

			workbook.close();
		} catch (Exception e) {
			throw new Exception("Failed to process Excel file: " + e.getMessage());
		}

		System.out.println("Timetables to be saved: " + timetables.size());
		return timetables;
	}

	public boolean isRoomInfoRow(Row row) {
		return row.getCell(1) != null && row.getCell(1).getStringCellValue().startsWith("Phòng:");
	}

	public Room extractRoomFromExcel(String roomInfo) {
		String roomName = "";
		String location = "";
		int capacity = 0;

		if (roomInfo != null && roomInfo.contains(":") && roomInfo.contains("(")) {
			// Lấy tên phòng: "LA1.604"
			roomName = roomInfo.substring(roomInfo.indexOf(":") + 1, roomInfo.indexOf("(")).trim();

			// Tách phần location (ví dụ: "A1" từ "LA1.604")
			location = extractLocationFromRoomName(roomName);

			// Lấy sức chứa: "35"
			String capacityStr = roomInfo.substring(roomInfo.indexOf("sức chứa :") + 11, roomInfo.indexOf(")")).trim();
			try {
				capacity = Integer.parseInt(capacityStr);
			} catch (NumberFormatException e) {
				System.out.println("Error parsing capacity: " + capacityStr);
			}
		}

		Room room = roomRepository.findByName(roomName);
		if (room == null) {
			room = Room.builder()
					.name(roomName)
					.location(location) // Gán location (ví dụ: "A1")
					.capacity(capacity)
					.status(RoomStatus.AVAILABLE)
					.build();
			room = roomRepository.save(room);
		}

		return room;
	}

	private String extractLocationFromRoomName(String roomName) {
		// Giả định rằng roomName có định dạng kiểu "LA1.604" và bạn muốn trích xuất "A1"
		StringBuilder letters = new StringBuilder();
		StringBuilder numbers = new StringBuilder();

		for (int i = 0; i < roomName.length(); i++) {
			char c = roomName.charAt(i);
			if (Character.isLetter(c)) {
				letters.append(c);
			} else if (Character.isDigit(c)) {
				numbers.append(c);
			}
		}

		if (letters.length() >= 2 && !numbers.isEmpty()) {
			return letters.charAt(1) + numbers.substring(0, 1); // Trả về "A1"
		}
		return roomName;
	}

	public Semester extractSemesterFromExcel(Row row) {
		Semester currentSemester = null;
		String semesterName = null;
		String academicYear = null;
		if (row.getRowNum() <= 10) {
			String semesterInfo = getStringCellValue(row, 3);

			if (!semesterInfo.trim().isEmpty()) {
				String[] lines = semesterInfo.split("\\r?\\n");

				// Tìm dòng có chứa "Học kỳ" và "Năm học"
				for (String line : lines) {
					if (line.toLowerCase().contains("học kỳ") && line.toLowerCase().contains("năm học")) {
						semesterInfo = line.replace("Năm học", "").trim();

						String[] parts = semesterInfo.split("-");

						if (parts.length == 3) {
							semesterName = parts[0].replace("Học kỳ", "Semester").trim();  // "Học kỳ 1"
							academicYear = parts[1].trim() + " - " + parts[2].trim();
						} else {
							System.out.println("Invalid semester format (parts issue): " + semesterInfo);
						}
						break;
					}
				}
			}

			if (semesterName != null) {
				currentSemester = Semester.builder()
						.name(semesterName)
						.academic_year(academicYear)
						.build();
				currentSemester = semesterRepository.save(currentSemester);

			}
		}
		return currentSemester;
	}

	public String[] getFirstNameAndLastNameFromFullName(String fullName) {
		String[] nameParts = fullName.split(" ");
		String firstName = nameParts[nameParts.length - 1];  // Tên
		String lastName = String.join(" ",
				Arrays.copyOfRange(nameParts, 0, nameParts.length - 1));  // Họ và tên lót
		return new String[]{firstName, lastName};
	}

	public DayOfWeek convertDayToDayOfWeekFromExcel(int dayOfWeekNumber) {
		return switch (dayOfWeekNumber) {
			case 2 -> DayOfWeek.MONDAY;
			case 3 -> DayOfWeek.TUESDAY;
			case 4 -> DayOfWeek.WEDNESDAY;
			case 5 -> DayOfWeek.THURSDAY;
			case 6 -> DayOfWeek.FRIDAY;
			case 7 -> DayOfWeek.SATURDAY;
			default -> throw new IllegalArgumentException(
					"Invalid value for DayOfWeek: " + dayOfWeekNumber);
		};
	}

	private String getStringCellValue(Row row, int cellIndex) {
		if (row.getCell(cellIndex) != null) {
			return switch (row.getCell(cellIndex).getCellType()) {
				case STRING -> row.getCell(cellIndex).getStringCellValue().trim();
				case NUMERIC -> String.valueOf((long) row.getCell(cellIndex).getNumericCellValue()).trim();
				default -> "";
			};
		}
		return "";
	}

	private double getNumericCellValue(Row row, int cellIndex) {
		if (row.getCell(cellIndex) != null) {
			if (row.getCell(cellIndex).getCellType() == CellType.NUMERIC) {
				return row.getCell(cellIndex).getNumericCellValue();
			} else if (row.getCell(cellIndex).getCellType() == CellType.STRING) {
				try {
					return Double.parseDouble(row.getCell(cellIndex).getStringCellValue());
				} catch (NumberFormatException e) {
					System.out.println(
							"Cannot parse string to numeric at row " + row.getRowNum() + ", column " + cellIndex);
					return 0;
				}
			}
		}
		return 0;
	}

	private boolean isIrrelevantTableRow(Row row) {
		if (row.getCell(0) != null) {
			String cellValue = row.getCell(0).getStringCellValue().toUpperCase();
			if (cellValue.contains("*SÁNG") || cellValue.contains("*CHIỀU") || cellValue.contains("*TỐI")) {
				// Kiểm tra xem bảng này đã được trích xuất chưa
				if (!cellValue.equals(lastExtractedSession)) {
					extractLessonTimes(row);
					lastExtractedSession = cellValue;
				}
				return true;
			}
		}
		return false;
	}

	private void extractLessonTimes(Row row) {
		String[] lessonData = getStringCellValue(row, 0).split("\\+");
		for (String lesson : lessonData) {
			lesson = lesson.trim();
			if (lesson.contains("Tiết")) {
				saveLessonTime(lesson);
			}
		}
	}

	private void saveLessonTime(String lessonInfo) {
		// Example lessonInfo: "Tiết 1: 08:00 - 08:50"
		// First, split the info to extract lesson number and time part
		String[] parts = lessonInfo.split(": ", 2);  // Split into "Tiết 1" and "08:00 - 08:50"

		if (parts.length < 2) {
			System.out.println("Invalid lesson info format: " + lessonInfo);
			return;
		}

		String lessonNumber = parts[0].replace("Tiết", "").trim();  // Get the lesson number
		String[] timeParts = parts[1].split("-");  // Get start and end times

		if (timeParts.length < 2) {
			System.out.println("Invalid time format in lesson info: " + lessonInfo);
			return;  // Skip if the time format is invalid
		}

		// Ensure the time parts are correctly formatted as "HH:mm"
		String startTimeStr = timeParts[0].trim();
		String endTimeStr = timeParts[1].trim();

		LocalTime startTime;
		LocalTime endTime;
		try {
			startTime = LocalTime.parse(startTimeStr);
			endTime = LocalTime.parse(endTimeStr);
		} catch (DateTimeParseException e) {
			System.out.println("Failed to parse time in lesson info: " + lessonInfo);
			return;
		}

		String session;
		int lessonNum = Integer.parseInt(lessonNumber);
		if (lessonNum >= 1 && lessonNum <= 6) {
			session = "SÁNG";
		} else if (lessonNum >= 7 && lessonNum <= 12) {
			session = "CHIỀU";
		} else {
			session = "TỐI";
		}

		LessonTime lessonTime = LessonTime.builder()
				.lessonNumber(lessonNum)
				.startTime(startTime)
				.endTime(endTime)
				.session(session)
				.build();

		lessonTimeList.add(lessonTime);
	}

	private void saveAllLessonTimes() {
		lessonTimeList.sort(Comparator.comparingInt(LessonTime::getLessonNumber));
		lessonTimeRepository.saveAll(lessonTimeList);
		lessonTimeList.clear();
	}

}
