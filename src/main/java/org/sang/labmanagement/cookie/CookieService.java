package org.sang.labmanagement.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

	@Value("${application.security.jwt.expiration}")
	private long jwtExpiration;

	@Value("${application.security.jwt.refresh-token.expiration}")
	private long refreshExpiration;

	public void addCookie(HttpServletResponse response, String name, String value, Integer maxAge) {
		Cookie cookie = new Cookie(name, value);
		cookie.setHttpOnly(false);   // Chặn truy cập từ JavaScript
		cookie.setSecure(false);     // Chỉ gửi qua HTTPS (bật nếu có SSL)
		cookie.setPath("/");        // Áp dụng cho toàn bộ API
		cookie.setMaxAge(
				maxAge != null ? maxAge : (int) (name.equals("access_token") ? jwtExpiration : refreshExpiration));
		response.addCookie(cookie);
	}

	public String getCookieValue(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(name)) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	public void deleteCookie(HttpServletResponse response, String name) {
		Cookie cookie = new Cookie(name, null);
		cookie.setHttpOnly(true);
		cookie.setSecure(false);
		cookie.setPath("/");
		cookie.setMaxAge(0); // Hết hạn ngay lập tức
		response.addCookie(cookie);
	}


}
