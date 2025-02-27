package org.sang.labmanagement.notification;

import java.nio.file.AccessDeniedException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;


	@PostMapping("/broadcast")
	public ResponseEntity<?> sendNotificationToAll(@RequestBody Notification notification) {
		notificationService.sendNotificationToAllUsers(notification.getTitle(), notification.getMessage());
		return ResponseEntity.ok("Notification sent to all users");
	}


	@GetMapping("/unread")
	public ResponseEntity<List<Notification>> getUnreadNotifications(Authentication connectedUser) {
		User user = (User) connectedUser.getPrincipal();
		return ResponseEntity.ok(notificationService.getUnreadNotifications(user));
	}


	@GetMapping
	public ResponseEntity<PageResponse<Notification>> getAllNotifications(Authentication connectedUser,
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
			) {
		User user = (User) connectedUser.getPrincipal();
		return ResponseEntity.ok(notificationService.getAllNotifications(user,page,size));
	}


	@PostMapping("/{id}/read")
	public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication connectedUser)
			throws AccessDeniedException {
		User user = (User) connectedUser.getPrincipal();
		notificationService.markAsRead(id, user);
		return ResponseEntity.ok("Notification marked as read");
	}

}
