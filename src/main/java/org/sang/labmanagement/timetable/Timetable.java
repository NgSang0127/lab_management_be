package org.sang.labmanagement.timetable;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sang.labmanagement.course.Course;
import org.sang.labmanagement.enrollment.Enrollment;
import org.sang.labmanagement.room.Room;
import org.sang.labmanagement.semester.Semester;
import org.sang.labmanagement.timetable.LessonTime.LessonTime;
import org.sang.labmanagement.user.instructor.Instructor;
import org.sang.labmanagement.user.student.Student;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Timetable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private DayOfWeek dayOfWeek;

	@ManyToOne
	@JoinColumn(name = "room_id")
	private Room room;


	@OneToMany(mappedBy = "timetable")
	private Set<Enrollment> enrollments;


	@ManyToMany(mappedBy = "timetables")
	@JsonIgnoreProperties("timetables")// Bỏ qua trường timetables khi trả về Course
	private Set<Course> courses;


	@ManyToOne
	@JoinColumn(name = "instructor_id", nullable = false)
	private Instructor instructor;

	private int numberOfStudents;

	private int startLesson;

	private int totalLessonSemester;

	private int totalLessonDay;

	private String classId;

	//"03/10/2024 - 24/10/2024"
	private String studyTime;


	@ManyToOne
	@JoinColumn(name = "start_lesson_time_id")
	private LessonTime startLessonTime;

	@ManyToOne
	@JoinColumn(name = "end_lesson_time_id")
	private LessonTime endLessonTime;
}
