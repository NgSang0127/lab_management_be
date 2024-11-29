package org.sang.labmanagement.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

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

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain)
			throws ServletException, IOException {

		// Bỏ qua các request liên quan đến authentication
		if (request.getServletPath().contains("/api/v1/auth")) {
			filterChain.doFilter(request, response);
			return;
		}

		// Kiểm tra xem có header Authorization không và nếu có, lấy token từ đó
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization header is missing or invalid");
			return;
		}

		final String jwt = authHeader.substring(7).trim();
		try {
			String username = jwtService.extractUsername(jwt);
			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				// Tải thông tin người dùng từ username
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);

				// Kiểm tra xem token có còn hợp lệ không (chưa hết hạn và chưa bị thu hồi)
				boolean isTokenValid = tokenRepository.findByToken(jwt)
						.map(t -> !t.isExpired() && !t.isRevoked())
						.orElse(false);

				if (jwtService.isTokenValid(jwt, userDetails) && isTokenValid) {
					// Xác thực token nếu hợp lệ
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				} else {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired or invalid");
					return;
				}
			}
		} catch (ExpiredJwtException e) {
			// Trả về mã lỗi 401 cho token hết hạn
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
			return;
		} catch (Exception e) {
			// Xử lý lỗi khác, có thể log lỗi
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
			return;
		}

		// Tiếp tục với chuỗi bộ lọc
		filterChain.doFilter(request, response);
	}
}
