package org.sang.labmanagement.security.token;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<Token,Long> {

	@Query("""
      select t from Token t
      where t.user.id = :id and t.expired = false and t.revoked = false
      """)
	List<Token> findAllValidTokenByUser(Long id);


	Optional<Token> findByToken(String token);


	@Transactional
	@Modifying
	@Query("DELETE FROM Token t WHERE t.expired = true OR t.revoked = true")
	void deleteExpiredOrRevokedTokens();
}
