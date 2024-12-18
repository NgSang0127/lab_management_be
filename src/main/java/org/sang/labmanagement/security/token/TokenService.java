package org.sang.labmanagement.security.token;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {
	private final TokenRepository tokenRepository;

	@Scheduled(cron = "0 0 0 * * *")
	public void cleanupExpiredOrRevokedTokens() {
		tokenRepository.deleteExpiredOrRevokedTokens();
		System.out.println("Expired or revoked tokens have been cleaned up.");
	}

}
