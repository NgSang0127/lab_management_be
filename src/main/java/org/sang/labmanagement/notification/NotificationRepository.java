package org.sang.labmanagement.notification;

import java.util.List;
import org.sang.labmanagement.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long> {
	List<Notification> findByUserAndStatus(User user, NotificationStatus status);
	Page<Notification> findByUser(User user,Pageable pageable);
}
