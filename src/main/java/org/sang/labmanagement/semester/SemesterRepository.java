package org.sang.labmanagement.semester;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SemesterRepository extends JpaRepository<Semester,Long> {
	Optional<Semester>findByNameAndAcademicYear(String name, String academicYear);
}
