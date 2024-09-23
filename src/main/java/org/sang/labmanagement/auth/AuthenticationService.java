package org.sang.labmanagement.auth;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.sang.labmanagement.auth.response.AuthenticationResponse;
import org.sang.labmanagement.auth.request.LoginRequest;
import org.sang.labmanagement.auth.request.RegistrationRequest;
import org.sang.labmanagement.user.User;

public interface AuthenticationService {

	void saveUserToken(User user,String accessToken,String refreshToken);

	AuthenticationResponse register(RegistrationRequest request) throws MessagingException;

	void sendValidationEmail(User user) throws MessagingException;


	AuthenticationResponse login(LoginRequest request);

	void activateAccount(String code) throws MessagingException;

	AuthenticationResponse refreshToken(HttpServletRequest request,
			HttpServletResponse response);

}
