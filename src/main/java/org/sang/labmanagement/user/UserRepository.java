package org.sang.labmanagement.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository  extends JpaRepository<User,Long>,JpaSpecificationExecutor<User> {

	Page<User> findAll(Pageable pageable);
	Optional<User> findUserById(Long id);
	Optional<User>findByUsername(String username);
	Optional<User>findByEmail(String email);

	@Query("SELECT u FROM User u WHERE u.enabled = false AND u.createdDate < :expirationTime")
	List<User> findUnverifiedUsersBefore(@Param("expirationTime") LocalDateTime expirationTime);
}
