package org.sang.labmanagement.enrollment;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sang.labmanagement.course.Course;
import org.sang.labmanagement.timetable.Timetable;
import org.sang.labmanagement.user.student.Student;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class Enrollment {

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;

	private LocalDateTime enrollmentDate;

	@Enumerated(EnumType.STRING)
	private EnrollStatus status;

	@ManyToOne
	@JoinColumn(name = "student_id")
	private Student student;

	@ManyToOne
	@JoinColumn(name = "course_id")
	private Course course;


	@ManyToOne
	@JoinColumn(name = "timetable_id")
	private Timetable timetable;
}
