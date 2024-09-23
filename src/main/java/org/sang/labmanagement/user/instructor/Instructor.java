package org.sang.labmanagement.user.instructor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sang.labmanagement.course.Course;
import org.sang.labmanagement.timetable.Timetable;
import org.sang.labmanagement.user.User;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class Instructor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String instructorId;

	@Enumerated(EnumType.STRING)
	private Department department;

	@JsonManagedReference
	@OneToOne
	@JoinColumn(name = "user_id")
	private User user;

	@JsonIgnore
	@OneToMany(mappedBy = "instructor",cascade = CascadeType.ALL)
	private Set<Course> courses;


	@JsonIgnore
	@OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL)
	private Set<Timetable> timetables;

}
