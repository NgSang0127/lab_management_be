package org.sang.labmanagement.activity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

	// Tìm các phiên đang hoạt động (không có endTime)
	@Query("SELECT log FROM UserActivityLog log WHERE log.user.username = :username AND log.endTime IS NULL")
	List<UserActivityLog> findOngoingSessions(@Param("username") String username);

	// Tổng thời gian sử dụng theo ngày
	@Query("SELECT SUM(log.duration) FROM UserActivityLog log WHERE log.user.username = :username AND log.startTime >= :startOfDay AND log.startTime < :endOfDay")
	Long getTotalUsageTimeByDay(@Param("username") String username,
			@Param("startOfDay") LocalDateTime startOfDay,
			@Param("endOfDay") LocalDateTime endOfDay);

	// Tìm các phiên cần dọn dẹp (không kết thúc và bắt đầu trước cutoffTime)
	@Query("SELECT log FROM UserActivityLog log WHERE log.endTime IS NULL AND log.startTime < :cutoffTime")
	List<UserActivityLog> findSessionsToCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);

	// Tìm các phiên theo người dùng và ngày
	List<UserActivityLog> findByUserAndDate(User user, LocalDate date);

	// Aggregation và Projection để lấy tổng thời gian sử dụng cho từng người dùng
	@Query("SELECT new org.sang.labmanagement.activity.UserTotalUsageDTO(log.user.id, log.user.username, SUM(log.duration)) " +
			"FROM UserActivityLog log " +
			"WHERE log.startTime BETWEEN :startOfDay AND :endOfDay " +
			"AND (:role IS NULL OR log.user.role = :role) " +
			"GROUP BY log.user.id, log.user.username " +
			"ORDER BY SUM(log.duration) DESC")
	Page<UserTotalUsageDTO> getTotalUsageTimeForUsersAcrossDays(
			@Param("startOfDay") LocalDateTime startOfDay,
			@Param("endOfDay") LocalDateTime endOfDay,
			@Param("role") Role role,
			Pageable pageable);


}
