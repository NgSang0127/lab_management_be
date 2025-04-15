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
		int expiration = maxAge != null ? maxAge : (int) (name.equals("access_token") ? jwtExpiration : refreshExpiration);
		String cookieValue = String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=None; Secure",
				name, value, expiration);
		response.addHeader("Set-Cookie", cookieValue);
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
		String cookieValue = String.format("%s=; Path=/; Max-Age=0; HttpOnly; SameSite=None; Secure", name);
		response.addHeader("Set-Cookie", cookieValue);
	}


}