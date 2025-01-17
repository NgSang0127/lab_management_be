package org.sang.labmanagement.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sang.labmanagement.user.User;

@Entity
@Table(name = "user_activity_log", indexes = {
		@Index(name = "idx_user_activity_user_id", columnList = "user_id"),
		@Index(name = "idx_user_activity_start_time", columnList = "start_time"),
		@Index(name = "idx_user_activity_end_time", columnList = "end_time"),
		@Index(name = "idx_user_activity_date", columnList = "date")
})
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Liên kết tới User
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// Thời gian bắt đầu phiên
	@Column(name = "start_time", nullable = false)
	private LocalDateTime startTime;

	// Thời gian kết thúc phiên
	@Column(name = "end_time")
	private LocalDateTime endTime;

	private Long duration = 0L;

	// Ngày hoạt động
	@Column(nullable = false)
	private LocalDate date;

	// Thời gian hoạt động cuối cùng trong phiên
	@Column(name = "last_activity_time")
	private LocalDateTime lastActivityTime;

}
