package org.sang.labmanagement.user.student;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sang.labmanagement.enrollment.Enrollment;
import org.sang.labmanagement.user.User;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class Student {
	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;

	private String studentId;

	private String major;

	private int yearOfStudy;

	@OneToOne
	@JoinColumn(name = "user_id",unique = true)
	private User user;

	@OneToMany(mappedBy = "student")
	private Set<Enrollment> enrollments;

}
