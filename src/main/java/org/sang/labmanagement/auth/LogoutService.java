package org.sang.labmanagement.auth;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.cookie.CookieService;
import org.sang.labmanagement.redis.BaseRedisServiceImpl;
import org.sang.labmanagement.security.jwt.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {

	private final JwtService jwtService;
	private final BaseRedisServiceImpl<String>redisService;
	private final CookieService cookieService;

	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		final String jwt;
		jwt = cookieService.getCookieValue(request, "access_token");

		// Add null check before extracting username
		if (jwt != null && !jwt.isEmpty()) {
			String username = jwtService.extractUsername(jwt);

			// Only proceed with blacklisting if we have a valid username
			if (username != null) {
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
		} else if (authHeader != null && authHeader.startsWith("Bearer ")) {
			// If no cookie token, try from Authorization header
			String headerToken = authHeader.substring(7);
			if (!headerToken.isEmpty()) {
				try {
					String username = jwtService.extractUsername(headerToken);
					if (username != null) {
						redisService.setWithExpiration("blacklist:access:" + headerToken, "revoked", 3600);

						String storedRefreshToken = redisService.get("refresh_token:" + username);
						if (storedRefreshToken != null) {
							redisService.setWithExpiration("blacklist:refresh:" + storedRefreshToken, "revoked", 3600);
							redisService.delete("refresh_token:" + username);
						}
					}
				} catch (Exception e) {
					// Log the error but don't throw it
					System.out.println("Error processing token during logout: " + e.getMessage());
				}
			}
		}

		// Always clear cookies regardless of token state
		cookieService.deleteCookie(response, "access_token");
		cookieService.deleteCookie(response, "refresh_token");
	}

}