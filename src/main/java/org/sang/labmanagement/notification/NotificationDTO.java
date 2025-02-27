package org.sang.labmanagement.notification;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
	private String title;
	private String message;
	private NotificationStatus status;
	private LocalDateTime createdDate;

}

