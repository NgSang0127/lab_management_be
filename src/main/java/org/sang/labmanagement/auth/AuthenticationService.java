package org.sang.labmanagement.auth;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.sang.labmanagement.auth.request.ChangePasswordRequest;
import org.sang.labmanagement.auth.request.ForgotPasswordRequest;
import org.sang.labmanagement.auth.request.ResetPasswordRequest;
import org.sang.labmanagement.auth.request.UpdateInformationUser;
import org.sang.labmanagement.auth.request.VerificationRequest;
import org.sang.labmanagement.auth.response.AuthenticationResponse;
import org.sang.labmanagement.auth.request.LoginRequest;
import org.sang.labmanagement.auth.request.RegistrationRequest;
import org.sang.labmanagement.user.User;
import org.springframework.security.core.Authentication;

public interface AuthenticationService {


	AuthenticationResponse register(RegistrationRequest request) throws MessagingException;

	void sendValidationEmail(String email, Locale locale) throws MessagingException;

	User findUser(Authentication connectedUser);

	AuthenticationResponse login(LoginRequest request,HttpServletResponse response);

	void activateAccount(String code,String email) throws MessagingException;

	AuthenticationResponse refreshToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException;

	boolean changePassword(ChangePasswordRequest request,Authentication connectedUser);

	String forgotPassword(ForgotPasswordRequest request) throws MessagingException;

	String resetPassword(ResetPasswordRequest request) throws MessagingException;

	String validateResetCode(ResetPasswordRequest request) throws MessagingException;

	boolean updateInformationUser(UpdateInformationUser request,Authentication connectedUser);

	AuthenticationResponse toggleTwoFactorAuthentication(Authentication connectedUser );

	AuthenticationResponse verifyOtpQR(String otpCode,String username,HttpServletResponse response);

	String verifyOtpByMail(String username);

	AuthenticationResponse verifyTFAEmail(VerificationRequest request,HttpServletResponse response);

}
