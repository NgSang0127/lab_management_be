package org.sang.labmanagement.notification;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final SimpMessagingTemplate messagingTemplate; // ✅ Inject SimpMessagingTemplate



	public void sendNotificationToAllUsers(String title, String message) {
		List<User> allUsers = userRepository.findAll();
		List<Notification> notifications = new ArrayList<>();

		for (User user : allUsers) {
			Notification notification = new Notification();
			notification.setUser(user);
			notification.setTitle(title);
			notification.setMessage(message);
			notification.setStatus(NotificationStatus.UNREAD);
			notifications.add(notification);
		}

		notificationRepository.saveAll(notifications);

		for (User user : allUsers) {
			messagingTemplate.convertAndSendToUser(
					user.getId().toString(), // Chuyển hướng đến từng user cụ thể
					"/notification",
					new NotificationDTO(title, message,NotificationStatus.UNREAD, LocalDateTime.now())
			);
		}
	}



	public List<Notification> getUnreadNotifications(User user) {
		return notificationRepository.findByUserAndStatus(user, NotificationStatus.UNREAD);
	}

	public PageResponse<Notification> getAllNotifications(User user,int page,int size) {
		Pageable pageable= PageRequest.of(page,size, Sort.by("createdDate").descending());
		Page<Notification> notifications=notificationRepository.findByUser(user,pageable);
		return PageResponse.<Notification>builder()
				.content(notifications.getContent())
				.number(notifications.getNumber())
				.size(notifications.getSize())
				.totalElements(notifications.getTotalElements())
				.totalPages(notifications.getTotalPages())
				.first(notifications.isFirst())
				.last(notifications.isLast())
				.build();
	}

	public void markAsRead(Long notificationId, User user) throws AccessDeniedException {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new RuntimeException("Notification not found"));

		System.out.println("id"+notification.getUser().getId()+"userid"+user.getId());
		if (!notification.getUser().getId().equals(user.getId())) {
			throw new AccessDeniedException("You cannot mark this notification as read");
		}

		notification.setStatus(NotificationStatus.READ);
		notificationRepository.save(notification);
	}




	public void sendNotificationToAll(Notification notification) {
		System.out.println("Sending notification: " + notification.getTitle());
		messagingTemplate.convertAndSend("/topic/notification", notification);
	}



}
