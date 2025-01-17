package org.sang.labmanagement.activity;

import java.time.Instant;
import java.util.List;
import org.sang.labmanagement.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityEventRepository extends JpaRepository<UserActivityEvent,Long> {
	List<UserActivityEvent> findByUserAndTimestampBetweenOrderByTimestampAsc(User user, Instant start, Instant end);
}
