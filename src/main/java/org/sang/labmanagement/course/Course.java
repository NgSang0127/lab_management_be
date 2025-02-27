package org.sang.labmanagement.course;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sang.labmanagement.enrollment.Enrollment;
import org.sang.labmanagement.semester.Semester;
import org.sang.labmanagement.timetable.Timetable;
import org.sang.labmanagement.user.instructor.Instructor;
import org.sang.labmanagement.user.student.Student;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class Course {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	@Column(length = 522)
	private String code;


	private String NH;

	private String TH;


	private String description;

	private int credits;

	@OneToMany(mappedBy = "course")
	private Set<Enrollment> enrollments;

	@ManyToOne
	@JoinColumn(name = "instructor_id",nullable = false)
	@JsonIgnore
	private Instructor instructor;

	@ManyToMany
	@JoinTable(
			name = "timetable_course",
			joinColumns = @JoinColumn(name = "course_id"),
			inverseJoinColumns = @JoinColumn(name = "timetable_id")
	)
	@JsonIgnoreProperties("courses")//// Bỏ qua trường courses khi trả về Timetable
	private Set<Timetable> timetables;


}
