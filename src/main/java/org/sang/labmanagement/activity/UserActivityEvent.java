package org.sang.labmanagement.activity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import org.sang.labmanagement.user.User;

@Entity
@Table(name = "user_activity_events", indexes = {
		@Index(name = "idx_user_activity_event_user_id", columnList = "user_id"),
		@Index(name = "idx_user_activity_event_timestamp", columnList = "timestamp"),
		@Index(name = "idx_user_activity_event_event_type", columnList = "event_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// Loại sự kiện (ví dụ: click, scroll, login, logout, etc.)
	@Column(name = "event_type", nullable = false)
	private String eventType;

	@Column(nullable = false)
	private Instant timestamp;
}