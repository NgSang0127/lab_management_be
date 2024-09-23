package org.sang.labmanagement.user.instructor;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor,Long> {
	Optional<Instructor>findByInstructorId(String id);
}
