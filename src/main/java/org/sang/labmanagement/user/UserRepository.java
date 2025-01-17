package org.sang.labmanagement.user;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository  extends JpaRepository<User,Long>,JpaSpecificationExecutor<User> {

	Page<User> findAll(Pageable pageable);
	Optional<User> findUserById(Long id);
	Optional<User>findByUsername(String username);
	Optional<User>findByEmail(String email);
}
