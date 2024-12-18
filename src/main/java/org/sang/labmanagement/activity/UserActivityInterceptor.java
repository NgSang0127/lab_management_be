package org.sang.labmanagement.activity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class UserActivityInterceptor implements HandlerInterceptor {

	private final UserActivityLogService userActivityLogService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;

		// Kiểm tra nếu có tên người dùng thì mới gọi startSession
		if (username != null) {
			userActivityLogService.startSession(username);
		}

		return true;
	}

}
