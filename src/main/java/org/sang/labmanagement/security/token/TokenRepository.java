package org.sang.labmanagement.security.token;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<Token,Long> {

	Optional<Token> findByToken(String token);

}
