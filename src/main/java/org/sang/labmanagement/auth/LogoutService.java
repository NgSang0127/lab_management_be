package org.sang.labmanagement.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.redis.BaseRedisServiceImpl;
import org.sang.labmanagement.security.jwt.JwtService;
import org.sang.labmanagement.security.token.TokenRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {

	private final JwtService jwtService;
	private final BaseRedisServiceImpl<String>redisService;

	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		final String jwt;
		if(authHeader == null || !authHeader.startsWith("Bearer ")){
			return;
		}
		jwt=authHeader.substring(7);
		String username = jwtService.extractUsername(jwt);

		// Xóa Access Token khỏi Redis (Thêm vào danh sách đen)
		redisService.setWithExpiration("blacklist:access:" + jwt, "revoked", 3600);

		String storedRefreshToken = redisService.get("refresh_token:" + username);

		if (storedRefreshToken != null) {
			// Thêm Refresh Token vào blacklist
			redisService.setWithExpiration("blacklist:refresh:" + storedRefreshToken, "revoked", 3600);
			// Xóa khỏi Redis
			redisService.delete("refresh_token:" + username);
		}
	}
}
