package org.sang.labmanagement.logs;

import java.time.LocalDateTime;
import java.util.List;
import org.sang.labmanagement.course.Course;
import org.sang.labmanagement.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogsRepository extends JpaRepository<Logs,Long> {
	Page<Logs> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

	List<Logs> findByAction(String action);

	List<Logs> findByCourse(Course course);

	List<Logs> findByUser(User user);


}
