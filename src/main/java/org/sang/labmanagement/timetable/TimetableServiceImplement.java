package org.sang.labmanagement.timetable;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.sang.labmanagement.course.Course;
import org.sang.labmanagement.course.CourseRepository;
import org.sang.labmanagement.room.Room;
import org.sang.labmanagement.room.RoomRepository;
import org.sang.labmanagement.room.RoomStatus;
import org.sang.labmanagement.semester.Semester;
import org.sang.labmanagement.semester.SemesterRepository;
import org.sang.labmanagement.timetable.lesson_time.LessonTime;
import org.sang.labmanagement.timetable.lesson_time.LessonTimeRepository;
import org.sang.labmanagement.timetable.request.CreateTimetableRequest;
import org.sang.labmanagement.user.Role;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.sang.labmanagement.user.instructor.Department;
import org.sang.labmanagement.user.instructor.Instructor;
import org.sang.labmanagement.user.instructor.InstructorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TimetableServiceImplement implements TimetableService {

	private String lastExtractedSession = "";  // Biến lưu trạng thái lần trích xuất gần nhất
	private final List<LessonTime> lessonTimeList = new ArrayList<>();
	private String previousCode = "";  // Mã MH
	private int previousCredits = 0;   // Số TC
	private String previousNH = "";        // NH
	private String previousTH = "";        // TH
	private String previousClassId = ""; // Lớp
	private int previousNumberOfStudents = 0;

	private final TimetableRepository timetableRepository;
	private final RoomRepository roomRepository;
	private final InstructorRepository instructorRepository;
	private final UserRepository userRepository;
	private final CourseRepository courseRepository;
	private final SemesterRepository semesterRepository;
	private final LessonTimeRepository lessonTimeRepository;
	private final PasswordEncoder passwordEncoder;



	@Override
	public List<Timetable> getAllTimetableByWeek(LocalDate startDate, LocalDate endDate) {
		List<Timetable> matchingTimetables = new ArrayList<>();

		List<Timetable> allTimetables = timetableRepository.findAll();

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

	@Override
	public Map<String, String> getFirstAndLastWeek(Long semesterId) {
		List<Timetable> timetables = timetableRepository.findBySemesterId(semesterId); // Tìm thời khóa biểu theo kỳ học

		for (Timetable timetable: timetables
			 ) {
			System.out.println("timetable"+timetable);
		}
		System.out.println("Total timetables found: " + timetables.size());

		if (timetables.isEmpty()) {
			System.out.println("No timetables found for semesterId: " + semesterId);
			return Collections.emptyMap();
		}

		LocalDate minDate = LocalDate.MAX;
		LocalDate maxDate = LocalDate.MIN;

		for (Timetable timetable : timetables) {
			List<LocalDate[]> periods = extractStudyPeriods(timetable.getStudyTime());

			for (LocalDate[] period : periods) {
				LocalDate periodStart = period[0];
				LocalDate periodEnd = period[1];

				if (periodStart.isBefore(minDate)) {
					minDate = periodStart;
				}
				if (periodEnd.isAfter(maxDate)) {
					maxDate = periodEnd;
				}
			}
		}

		if (minDate == LocalDate.MAX || maxDate == LocalDate.MIN) {
			System.out.println("No valid study periods found for semesterId: " + semesterId);
			return Collections.emptyMap();
		}

		LocalDate firstWeekStart = getStartOfWeek(minDate);
		LocalDate lastWeekEnd = getEndOfWeek(maxDate);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		Map<String, String> result = new HashMap<>();
		result.put("firstWeekStart", firstWeekStart.format(formatter));
		result.put("lastWeekEnd", lastWeekEnd.format(formatter));

		return result;
	}


	// Hàm để lấy ngày bắt đầu của tuần từ một LocalDate (Thứ Hai)
	public LocalDate getStartOfWeek(LocalDate date) {
		return date.with(DayOfWeek.MONDAY); // Trả về ngày Thứ Hai của tuần
	}

	// Hàm để lấy ngày kết thúc của tuần từ một LocalDate (Chủ Nhật)
	public LocalDate getEndOfWeek(LocalDate date) {
		return date.with(DayOfWeek.SUNDAY); // Trả về ngày Chủ Nhật của tuần
	}

	@Override
	public boolean cancelTimetableOnDate(LocalDate cancelDate, int startLesson, String roomName, Long timetableId) {
		Timetable timetable=timetableRepository.findById(timetableId).orElseThrow(
				()->new IllegalArgumentException("Not found timetable with id:"+timetableId)
		);
		List<LocalDate[]> studyPeriods = extractStudyPeriods(timetable.getStudyTime());

		//Kiễm tra xem ngày cần hủy có nằm trong bất kỳ khoảng thời gian nào không
		for(LocalDate[] period :studyPeriods){
			LocalDate startDate=period[0];
			LocalDate endDate=period[1];

			if ((cancelDate.isEqual(startDate) || cancelDate.isAfter(startDate))
					&& (cancelDate.isEqual(endDate) || cancelDate.isBefore(endDate))
			) {

				if(timetable.getStartLesson()==startLesson && timetable.getRoom().getName().equals(roomName)){
					timetable.getCancelDates().add(cancelDate);
					timetableRepository.save(timetable);
					return true;
				}
			}
		}
		return false;
	}

	@Override
// Lấy danh sách Timetable dựa trên ngày
	public List<Timetable> getTimetablesByDate(LocalDate date) {
		List<Timetable> timetables = timetableRepository.findAll();

		return timetables.stream()
				.filter(timetable -> isCorrectDayAndPeriod(timetable, date) && !isDateCanceled(timetable, date))
				.collect(Collectors.toList());
	}

	// Kiểm tra xem ngày có thuộc thứ (DayOfWeek) và nằm trong khoảng thời gian học không
	private boolean isCorrectDayAndPeriod(Timetable timetable, LocalDate date) {
		// Kiểm tra thứ trong tuần
		if (!timetable.getDayOfWeek().equals(date.getDayOfWeek())) {
			return false; // Nếu ngày đó không trùng thứ với timetable thì bỏ qua
		}

		// Kiểm tra xem ngày có nằm trong khoảng thời gian học không
		List<LocalDate[]> studyPeriods = extractStudyPeriods(timetable.getStudyTime());

		// Sử dụng for để kiểm tra từng khoảng thời gian
		for (LocalDate[] period : studyPeriods) {
			LocalDate startDate = period[0];
			LocalDate endDate = period[1];

			// Kiểm tra nếu ngày nằm trong khoảng từ startDate đến endDate
			if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
				return true; // Ngày nằm trong khoảng thời gian hợp lệ
			}
		}

		return false; // Ngày không nằm trong bất kỳ khoảng thời gian học nào
	}

	// Kiểm tra xem ngày có nằm trong danh sách ngày đã bị hủy không
	private boolean isDateCanceled(Timetable timetable, LocalDate date) {
		if (timetable.getCancelDates() == null || timetable.getCancelDates().isEmpty()) {
			return false;
		}
		return timetable.getCancelDates().contains(date);
	}



	@Override
	public Timetable getTimetableByCourse(String courseId, String NH, String TH,String studyTime) {
		// Tìm timetable dựa trên thông tin môn học
		System.out.println(timetableRepository.findByCourseAndStudyTime(courseId, NH, TH,studyTime));
		return timetableRepository.findByCourseAndStudyTime(courseId, NH, TH,studyTime);
	}

	@Override
	public Timetable getTimetableByTimetableName(String timetableName) {
		// Tìm timetable dựa trên tên thời khóa biểu
		return timetableRepository.findByTimetableName(timetableName);
	}


	@Override
	public Timetable createTimetable(CreateTimetableRequest request) {
		Room room = roomRepository.findByName(request.getRoomName());

		Instructor instructor = instructorRepository.findByInstructorId(request.getInstructorId()).orElseThrow(
				() -> new RuntimeException("Instructor not found")
		);

		LessonTime startTime = lessonTimeRepository.findByLessonNumber(request.getStartLesson());

		LessonTime endTime = lessonTimeRepository.findByLessonNumber(request.getEndLesson());

		LocalDate date = request.getDate();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");


		String formattedDate = date.format(formatter);

		Timetable timetable = Timetable.builder()
				.timetableName(request.getTimetableName())
				.room(room)
				.startLesson(startTime.getLessonNumber())
				.startLessonTime(startTime)
				.endLessonTime(endTime)
				.instructor(instructor)
				.totalLessonDay(request.getEndLesson() - request.getStartLesson() + 1)
				.dayOfWeek(date.getDayOfWeek())
				.studyTime(formattedDate)
				.description(request.getDescription())
				.build();

		return timetableRepository.save(timetable);
	}


	@Override
	@Transactional
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
				String code = getMergedCellValue(row, 0);
				code = code.isEmpty() ? previousCode : (previousCode = code);
				int credits = (int) getNumericCellValue(row, 7);
				credits = (credits == 0) ? previousCredits : (previousCredits = credits);
				String NH = getMergedCellValue(row, 8);
				if (!NH.isEmpty()) {
					previousNH = NH;
				} else {
					NH = "0";
				}

				String TH = getMergedCellValue(row, 9);
				if (!TH.isEmpty()) {
					previousTH = TH;
				} else {
					TH = "0";
				}
				String classId = getMergedCellValue(row, 10);
				classId = classId.isEmpty() ? previousClassId : (previousClassId = classId);
				String numberOfStudentsStr = getMergedCellValue(row, 11);
				int numberOfStudents = 0;
				try {
					// Chuyển đổi từ String sang int nếu có giá trị hợp lệ
					numberOfStudents = Integer.parseInt(numberOfStudentsStr);
				} catch (NumberFormatException e) {
					// Nếu không thể chuyển đổi, sử dụng giá trị mặc định hoặc xử lý ngoại lệ
					System.out.println("Invalid number format: " + numberOfStudentsStr);
				}
				if (numberOfStudents >= 0) {
					previousNumberOfStudents = numberOfStudents;
				} else {
					numberOfStudents = previousNumberOfStudents;
				}


				// Xử lý FullName giảng viên
				String fullName = getStringCellValue(row, 21).trim();
				String[] names = getFirstNameAndLastNameFromFullName(fullName);
				String firstName = names[0];
				String lastName = names[1];

				String username = getStringCellValue(row, 19);
				User savedUser = userRepository.findByUsername(username).orElse(null);
				if (savedUser == null) {
					// Nếu user không tồn tại, kiểm tra thông tin trước khi lưu
					if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
						User user = User.builder()
								.firstName(firstName)
								.lastName(lastName)
								.password(passwordEncoder.encode(username))
								.enabled(true)
								.username(username) // Mã viên chức
								.role(Role.TEACHER)
								.build();

						// Lưu user mới
						savedUser = userRepository.save(user);
					}
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
				// Kiểm tra nếu mã môn học và tên môn học trùng với dòng trước
				System.out.println("Checking course with Code: " + code + ", NH: " + NH + ", TH: " + TH);
				Course savedCourse = courseRepository.findByCodeAndNHAndTH(code, NH, TH).orElse(null);

				if (savedCourse == null) {
					System.out.println("Course not found. Creating new course...");
					savedCourse = Course.builder()
							.code(code)
							.name(getStringCellValue(row, 2)) // Tên môn học
							.credits(credits)
							.NH(NH)
							.timetables(new HashSet<>())
							.TH(TH)
							.instructor(savedInstructor)
							.build();
					savedCourse = courseRepository.save(savedCourse);
					System.out.println("New course saved: " + savedCourse.getCode() + " - " + savedCourse.getName());
				} else {
					System.out.println("Course already exists: " + savedCourse.getCode() + " - " + savedCourse.getName());
				}


				// Lấy thông tin DayOfWeek từ Excel
				int dayOfWeekNumber = (int) getNumericCellValue(row, 12);
				DayOfWeek dayOfWeek = convertDayToDayOfWeekFromExcel(dayOfWeekNumber);
				int startLesson = (int) getNumericCellValue(row, 13);
				String studyTime=getStringCellValue(row, 16);

				assert currentRoom != null;
				Optional<Timetable> existingTimetable = timetableRepository.findByClassIdAndRoomNameAndStudyTimeAndTHAndNH(
						classId,currentRoom.getName(),studyTime,TH,NH);
				if(existingTimetable.isPresent()) {
					System.out.println(
							"Timetable already exists for Day: " + dayOfWeek + ", Start Lesson: " + startLesson
									+ ", Class ID: " + classId);
				} else{
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
							.studyTime(studyTime)
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
			}
			workbook.close();
		} catch (Exception e) {
			throw new Exception("Failed to process Excel file: " + e.getMessage());
		}

		System.out.println("Timetables to be saved: " + timetables.size());

		//Cập nhật endate
		if (currentSemester != null) {
			Map<String, String> firstAndLastWeek = getFirstAndLastWeek(currentSemester.getId());

			if (firstAndLastWeek.containsKey("lastWeekEnd")) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
				LocalDate endDate = LocalDate.parse(firstAndLastWeek.get("lastWeekEnd"), formatter);
				currentSemester.setEndDate(endDate);


				semesterRepository.save(currentSemester);
			}
	}
		return timetables;
	}

	@Override
	public List<Semester> getFourSemesterRecent() {
		return semesterRepository.findTop4ByOrderByStartDateDesc(PageRequest.of(0,4));
	}

	// HELPER METHOD
	private boolean isRoomInfoRow(Row row) {
		return row.getCell(1) != null && row.getCell(1).getStringCellValue().startsWith("Phòng:");
	}

	private Room extractRoomFromExcel(String roomInfo) {
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

	private Semester extractSemesterFromExcel(Row row) {
		Semester currentSemester = null;
		String semesterName = null;
		String academicYear = null;
		LocalDate startDate = null;

		if (row.getRowNum() <= 10) {
			String semesterInfo = getStringCellValue(row, 3);
			String startDateStr = getStringCellValue(row, 1);

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

			// Kiểm tra và parse ngày bắt đầu học kỳ
			if (!startDateStr.trim().isEmpty()) {
				Matcher matcher = Pattern.compile("\\d{2}/\\d{2}/\\d{4}").matcher(startDateStr);//patern dd/mm/yyyy
				if (matcher.find()) {
					String extractedDate = matcher.group();
					try {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
						startDate = LocalDate.parse(extractedDate, formatter);
						DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
						String formattedDate = startDate.format(outputFormatter);
					} catch (DateTimeParseException e) {
						System.out.println("Lỗi khi phân tích ngày bắt đầu học kỳ: " + extractedDate);
					}
				}
			}

			if (semesterName != null) {
				Optional<Semester> existingSemester = semesterRepository.findByNameAndAcademicYear(semesterName, academicYear);
				if (existingSemester.isPresent()) {
					currentSemester = existingSemester.get();
				} else {
					currentSemester = Semester.builder()
							.name(semesterName)
							.academicYear(academicYear)
							.startDate(startDate) // Lưu ngày bắt đầu học kỳ
							.build();
					currentSemester = semesterRepository.save(currentSemester);
				}
			}
		}
		return currentSemester;
	}

	private List<LocalDate[]> extractStudyPeriods(String studyTime) {
		List<LocalDate[]> periods = new ArrayList<>();

		// Tách chuỗi theo dòng mới "\n" nếu có nhiều khoảng thời gian hoặc ngày lẻ
		String[] periodStrings = studyTime.split("\n");

		for (String periodString : periodStrings) {
			String[] dates = periodString.split("-");

			if (dates.length == 2) { // Trường hợp khoảng thời gian
				LocalDate startDate = LocalDate.parse(dates[0].trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
				LocalDate endDate = LocalDate.parse(dates[1].trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
				periods.add(new LocalDate[]{startDate, endDate});
			} else if (dates.length == 1) { // Trường hợp chỉ có một ngày
				LocalDate singleDate = LocalDate.parse(dates[0].trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
				periods.add(new LocalDate[]{singleDate, singleDate}); // Tạo một khoảng mà ngày bắt đầu và kết thúc đều là cùng một ngày
			}
		}

		return periods;
	}

	private String[] getFirstNameAndLastNameFromFullName(String fullName) {
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

	private String getMergedCellValue(Row row, int cellIndex) {
		Cell cell = row.getCell(cellIndex);
		if (cell != null) {
			Sheet sheet = row.getSheet();
			// Iterate through all merged regions to check if the current cell is part of any
			for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
				CellRangeAddress range = sheet.getMergedRegion(i);
				if (range.isInRange(row.getRowNum(), cellIndex)) {
					// Retrieve the first cell of the merged region
					Row firstRow = sheet.getRow(range.getFirstRow());
					if (firstRow != null) {
						Cell firstCell = firstRow.getCell(range.getFirstColumn());
						if (firstCell != null) {
							return getStringCellValue(firstCell).trim();
						}
					}
				}
			}
			// If the cell is not part of any merged region, return its own value
			return getStringCellValue(cell).trim();
		}
		return "";
	}

	private String getStringCellValue(Cell cell) {
		if (cell == null) return "";
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue();
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(cell.getDateCellValue());
				}
				return String.valueOf((int) cell.getNumericCellValue());
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
				// Optionally, evaluate the formula and return the result
				FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
				CellValue cellValue = evaluator.evaluate(cell);
				return switch (cellValue.getCellType()) {
					case STRING -> cellValue.getStringValue();
					case NUMERIC -> String.valueOf((int) cellValue.getNumberValue());
					case BOOLEAN -> String.valueOf(cellValue.getBooleanValue());
					default -> "";
				};
			default:
				return "";
		}
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

		boolean exists = lessonTimeRepository.existsByLessonNumberAndStartTimeAndEndTime(lessonNum, startTime, endTime);

		if (exists) {
			System.out.println("LessonTime already exists: Tiết " + lessonNum + " (" + startTime + " - " + endTime + ")");
			return;
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
