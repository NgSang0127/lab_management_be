package org.sang.labmanagement.exception.handler;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.sang.labmanagement.exception.AccountAlreadyActivatedException;
import org.sang.labmanagement.exception.EmailCodeException;
import org.sang.labmanagement.exception.IllegalStateException;
import org.sang.labmanagement.exception.OperationNotPermittedException;
import org.sang.labmanagement.exception.QRCodeException;
import org.sang.labmanagement.exception.ResourceAlreadyExistsException;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.sang.labmanagement.exception.TokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private ResponseEntity<ExceptionResponse> buildResponse(
			HttpStatus status, Integer businessErrorCode, String businessErrorDescription, String errorMessage) {
		return ResponseEntity.status(status)
				.body(ExceptionResponse.builder()
						.businessErrorCode(businessErrorCode)
						.businessErrorDescription(businessErrorDescription)
						.error(errorMessage)
						.timestamp(LocalDateTime.now())
						.build());
	}


	/**
	 * Handles LockedException, typically thrown when an account is locked.
	 */
	@ExceptionHandler(LockedException.class)
	public ResponseEntity<ExceptionResponse> handleLockedException(LockedException exp) {
		log.warn("Account locked: {}", exp.getMessage());
		return buildResponse(HttpStatus.UNAUTHORIZED,
				BusinessErrorCodes.ACCOUNT_LOCKED.getCode(),
				BusinessErrorCodes.ACCOUNT_LOCKED.getDescription(),
				exp.getMessage());
	}

	/**
	 * Handles DisabledException, thrown when an account is disabled.
	 */
	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ExceptionResponse> handleDisabledException(DisabledException exp) {
		log.warn("Account disabled: {}", exp.getMessage());
		return buildResponse(HttpStatus.UNAUTHORIZED,
				BusinessErrorCodes.ACCOUNT_DISABLED.getCode(),
				BusinessErrorCodes.ACCOUNT_DISABLED.getDescription(),
				exp.getMessage());
	}


	/**
	 * Handles BadCredentialsException, indicating that authentication failed due to invalid credentials.
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ExceptionResponse> handleBadCredentials(BadCredentialsException exp) {
		log.warn("Bad credentials: {}", exp.getMessage());
		return buildResponse(HttpStatus.UNAUTHORIZED,
				BusinessErrorCodes.BAD_CREDENTIALS.getCode(),
				BusinessErrorCodes.BAD_CREDENTIALS.getDescription(),
				exp.getMessage());
	}

	/**
	 * Handles MessagingException, which may occur during email sending operations.
	 */
	@ExceptionHandler(MessagingException.class)
	public ResponseEntity<ExceptionResponse> handleMessagingException(MessagingException exp) {
		log.error("Email sending error: {}", exp.getMessage(), exp);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
				BusinessErrorCodes.EMAIL_SEND_FAILED.getCode(),
				BusinessErrorCodes.EMAIL_SEND_FAILED.getDescription(),
				exp.getMessage());
	}



	@ExceptionHandler(OperationNotPermittedException.class)
	public ResponseEntity<ExceptionResponse> handleOperationNotPermitted(OperationNotPermittedException exp) {
		log.warn("Operation not permitted: {}", exp.getMessage());
		return buildResponse(HttpStatus.FORBIDDEN,
				BusinessErrorCodes.UNAUTHORIZED.getCode(),
				BusinessErrorCodes.UNAUTHORIZED.getDescription(),
				exp.getMessage());
	}


	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ExceptionResponse> handleResourceNotFound(ResourceNotFoundException exp) {
		log.warn("Resource not found: {}", exp.getMessage());
		return buildResponse(HttpStatus.NOT_FOUND,
				BusinessErrorCodes.RESOURCE_NOT_FOUND.getCode(),
				BusinessErrorCodes.RESOURCE_NOT_FOUND.getDescription(),
				exp.getMessage());
	}

	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<ExceptionResponse> handleResourceAlreadyExists(ResourceAlreadyExistsException exp) {
		log.warn("Resource already exist : {}", exp.getMessage());
		return buildResponse(HttpStatus.CONFLICT,
				BusinessErrorCodes.RESOURCE_ALREADY_EXISTS.getCode(),
				BusinessErrorCodes.RESOURCE_ALREADY_EXISTS.getDescription(),
				exp.getMessage());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ExceptionResponse> handleIllegalState(IllegalStateException exp) {
		log.warn("Illegal state: {}", exp.getMessage());
		return buildResponse(HttpStatus.BAD_REQUEST,
				BusinessErrorCodes.ILLEGAL_STATE.getCode(),
				BusinessErrorCodes.ILLEGAL_STATE.getDescription(),
				exp.getMessage());
	}

	@ExceptionHandler(EmailCodeException.class)
	public ResponseEntity<ExceptionResponse> handleEmailCode(EmailCodeException exp){
		log.warn("Email code : {}",exp.getMessage());
		return buildResponse(HttpStatus.BAD_REQUEST,
				BusinessErrorCodes.INVALID_VERIFICATION_CODE.getCode(),
				BusinessErrorCodes.INVALID_VERIFICATION_CODE.getDescription(),
				exp.getMessage());
	}


	@ExceptionHandler(Exception.class)
	public ResponseEntity<ExceptionResponse> handleGeneralException(Exception exp) {
		log.error("Unhandled exception: {}", exp.getMessage(), exp);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
				BusinessErrorCodes.INTERNAL_SERVER_ERROR.getCode(),
				BusinessErrorCodes.INTERNAL_SERVER_ERROR.getDescription(),
				"Internal server error, please contact support.");
	}

	@ExceptionHandler(TokenException.class)
	public ResponseEntity<ExceptionResponse> handleTokenException(TokenException exp) {
		log.error("Token error: {}", exp.getMessage(), exp);
		return buildResponse(HttpStatus.UNAUTHORIZED,
				BusinessErrorCodes.TOKEN_INVALID.getCode(),
				BusinessErrorCodes.TOKEN_INVALID.getDescription(),
				exp.getMessage());
	}


	@ExceptionHandler(AccountAlreadyActivatedException.class)
	public ResponseEntity<ExceptionResponse> handleAccountAlreadyActivated(AccountAlreadyActivatedException exp) {
		log.warn("Account already activated: {}", exp.getMessage());
		return buildResponse(HttpStatus.BAD_REQUEST,
				BusinessErrorCodes.ACCOUNT_ALREADY_ACTIVATED.getCode(),
				BusinessErrorCodes.ACCOUNT_ALREADY_ACTIVATED.getDescription(),
				exp.getMessage());
	}

	@ExceptionHandler(QRCodeException.class)
	public ResponseEntity<ExceptionResponse> handleQRCodeException(QRCodeException exp) {
		log.warn("QR code: {}", exp.getMessage());
		return buildResponse(HttpStatus.UNAUTHORIZED,
				BusinessErrorCodes.INVALID_OTP.getCode(),
				BusinessErrorCodes.INVALID_OTP.getDescription(),
				exp.getMessage());
	}



}
