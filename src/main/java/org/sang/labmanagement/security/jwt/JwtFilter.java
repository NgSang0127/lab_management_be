package org.sang.labmanagement.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

import org.sang.labmanagement.redis.BaseRedisServiceImpl;
import org.sang.labmanagement.security.token.TokenRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

@Service
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final TokenRepository tokenRepository;
	private final BaseRedisServiceImpl<String> redisService;

	private static final String AUTH_PATH = "/api/v1/auth";
	private static final String WS_PATH = "/ws";

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain)
			throws ServletException, IOException {

		if (request.getServletPath().startsWith(WS_PATH) || request.getServletPath().contains(AUTH_PATH)) {
			filterChain.doFilter(request, response);
			return;
		}

		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			sendUnauthorizedResponse(response, "Missing or invalid Authorization header");
			return;
		}

		final String jwt = authHeader.substring(7).trim();
		try {
			String username = jwtService.extractUsername(jwt);
			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);

				if (isTokenValid(jwt, userDetails)) {
					authenticateUser(request, userDetails);
				} else {
					sendUnauthorizedResponse(response, "Invalid or revoked token");
					return;
				}
			}
		} catch (ExpiredJwtException e) {
			System.out.println("Access Token đã hết hạn: " + jwt);
			sendUnauthorizedResponse(response, "Token expired");
			return;
		} catch (Exception e) {
			System.out.println("Lỗi xác thực JWT: " + e.getMessage());
			sendUnauthorizedResponse(response, "Unauthorized");
			return;
		}

		filterChain.doFilter(request, response);
	}


	/**
	 * Kiểm tra token có hợp lệ và chưa bị thu hồi không.
	 */
	private boolean isTokenValid(String token, UserDetails userDetails) {
		if (redisService.get("blacklist:access:" + token) != null) {
			return false;
		}

		return jwtService.isTokenValid(token, userDetails);
	}


	/**
	 * Xác thực người dùng trong SecurityContext.
	 */
	private void authenticateUser(HttpServletRequest request, UserDetails userDetails) {
		UsernamePasswordAuthenticationToken authToken =
				new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authToken);
	}

	/**
	 * Trả về lỗi 401 - Unauthorized.
	 */
	private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
	}
}
