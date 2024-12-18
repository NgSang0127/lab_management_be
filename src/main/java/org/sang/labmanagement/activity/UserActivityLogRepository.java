package org.sang.labmanagement.activity;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.sang.labmanagement.user.Role;
import org.sang.labmanagement.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog,Long> {

	@Query("SELECT log FROM UserActivityLog log WHERE log.user.username = :username AND log.endTime IS NULL")
	List<UserActivityLog> findOngoingSessions(@Param("username") String username);


	@Query("SELECT SUM(log.duration) FROM UserActivityLog log WHERE log.user.username = :username AND log.startTime >= "
			+ ":startOfDay AND log.startTime < :endOfDay")
	Long getTotalUsageTimeByDay(@Param("username") String username,
			@Param("startOfDay") LocalDateTime startOfDay,
			@Param("endOfDay") LocalDateTime endOfDay);

	@Query("SELECT log FROM UserActivityLog log WHERE log.endTime IS NULL AND log.startTime < :cutoffTime")
	List<UserActivityLog> findSessionsToCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);

	List<UserActivityLog> findByUserAndDate(User user, LocalDate date);

	@Query("""
    SELECT log.user, log.startTime, log.endTime
    FROM UserActivityLog log
    WHERE log.startTime <= :endOfDay AND log.endTime >= :startOfDay
      AND (:role IS NULL OR log.user.role = :role)
""")
	List<Object[]> getUserActivityLogsForTimeRange(
			@Param("startOfDay") LocalDateTime startOfDay,
			@Param("endOfDay") LocalDateTime endOfDay,
			@Param("role") Role role
	);





}
