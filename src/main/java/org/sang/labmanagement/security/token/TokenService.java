package org.sang.labmanagement.security.token;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.redis.BaseRedisServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {
	private final BaseRedisServiceImpl<String> redisService;

	@Value("${application.security.jwt.refresh-token.expiration}")
	private long refreshExpiration;


	public void saveRefreshToken(String username, String refreshToken) {
		deleteRefreshToken(username);

		redisService.setWithExpiration("refresh_token:" + username, refreshToken, refreshExpiration);
	}


	public String getRefreshToken(String username) {
		return redisService.get("refresh_token:" + username);
	}

	public boolean isRefreshTokenValid(String username, String refreshToken) {
		String storedToken = redisService.get("refresh_token:" + username);
		return storedToken != null && storedToken.equals(refreshToken);
	}

	public void deleteRefreshToken(String username) {
		redisService.delete("refresh_token:" + username);
	}

	public void revokeRefreshToken(String username) {
		redisService.delete("refresh_token:" + username);
	}


	public boolean isRefreshTokenRevoked(String refreshToken) {
		return redisService.get("blacklist:refresh:" + refreshToken) != null;
	}





}
