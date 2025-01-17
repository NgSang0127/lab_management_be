package org.sang.labmanagement.logs;

import java.time.LocalDateTime;
import java.util.List;
import org.sang.labmanagement.course.Course;
import org.sang.labmanagement.logs.dto.CourseLogStatistics;
import org.sang.labmanagement.logs.dto.DailyLogStatistics;
import org.sang.labmanagement.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LogsRepository extends JpaRepository<Logs, Long> {

	Page<Logs> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

	List<Logs> findByAction(String action);

	List<Logs> findByCourse(Course course);

	List<Logs> findByUser(User user);

	@Query("SELECT new org.sang.labmanagement.logs.dto.DailyLogStatistics(CAST(l.timestamp AS java.time.LocalDate), COUNT(l)) " +
			"FROM Logs l WHERE l.timestamp BETWEEN :startDate AND :endDate " +
			"GROUP BY CAST(l.timestamp AS java.time.LocalDate) ORDER BY CAST(l.timestamp AS java.time.LocalDate)")
	List<DailyLogStatistics> findLogsGroupByDate(LocalDateTime startDate, LocalDateTime endDate);



	@Query("SELECT new org.sang.labmanagement.logs.dto.CourseLogStatistics(c.id, c.name, c.NH, c.TH, COUNT(l)) " +
			"FROM Logs l JOIN l.course c " +
			"WHERE l.timestamp BETWEEN :startDate AND :endDate " +
			"GROUP BY c.id, c.name, c.NH, c.TH " +
			"ORDER BY COUNT(l) DESC")
	List<CourseLogStatistics> findLogsGroupByCourse(LocalDateTime startDate, LocalDateTime endDate);



}
