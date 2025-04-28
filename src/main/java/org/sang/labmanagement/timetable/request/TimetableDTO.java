package org.sang.labmanagement.timetable.request;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.sang.labmanagement.course.CourseRequestDTO;

@Getter
@Setter
public class TimetableDTO {
	private Long id;
	private String timetableName; // Bắt buộc trong TH1, không cần trong TH2 và TH3
	@NotNull
	private Long semesterId;
	@NotNull
	private Long instructorId;
	@NotNull
	private DayOfWeek dayOfWeek;
	@NotNull
	private Long roomId;
	private Integer numberOfStudents;
	private Integer totalLessonSemester;
	private Integer totalLessonDay;
	private String classId;
	@NotNull
	private String studyTime;
	@NotNull
	private Long startLessonTimeId;
	@NotNull
	private Long endLessonTimeId;
	private String description;
	private List<CourseRequestDTO> courses; // Danh sách Course, cần trong TH2 và TH3
	private Set<LocalDate> cancelDates;

	// Validation logic
	public void validate() {
		if (timetableName == null && (courses == null || courses.isEmpty())) {
			throw new IllegalArgumentException("Either timetableName or courses must be provided");
		}
		if (courses != null && !courses.isEmpty()) {
			for (CourseRequestDTO course : courses) {
				if (course.getId() == null && // TH3: Tạo mới Course
						(course.getName() == null || course.getCode() == null || course.getNH() == null ||
								course.getTH() == null || course.getInstructorId() == null)) {
					throw new IllegalArgumentException("Course details (name, code, NH, TH, instructorId) must be provided for new courses");
				}
				if (course.getId() != null && course.getName() != null) {
					throw new IllegalArgumentException("For existing courses, only provide course ID");
				}
			}
		}
	}
}
