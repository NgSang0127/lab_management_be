package org.sang.labmanagement.activity;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

	private final UserActivityLogService userActivityLogService;

	/**
	 * Xử lý sự kiện đăng nhập thành công.
	 *
	 * @param event AuthenticationSuccessEvent
	 */
	@EventListener
	public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
		String username = event.getAuthentication().getName();
		userActivityLogService.startSession(username);
	}

	/**
	 * Xử lý sự kiện đăng xuất thành công.
	 *
	 * @param event LogoutSuccessEvent
	 */
	@EventListener
	public void handleLogoutSuccess(LogoutSuccessEvent event) {
		if (event.getAuthentication() != null) {
			String username = event.getAuthentication().getName();
			userActivityLogService.endSession(event.getAuthentication());
		}
	}
}
