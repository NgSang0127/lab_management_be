package org.sang.labmanagement.course;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course,Long> {

	Optional<Course> findByCodeAndNHAndTH(String code, int NH, int TH);
}
