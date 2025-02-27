package org.sang.labmanagement.semester;


import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SemesterRepository extends JpaRepository<Semester,Long> {
	Optional<Semester>findByNameAndAcademicYear(String name, String academicYear);

	@Query("SELECT s FROM Semester s ORDER BY s.startDate DESC")
	List<Semester> findTop4ByOrderByStartDateDesc(Pageable pageable);

}
