package org.sang.labmanagement.exception.handler;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.sang.labmanagement.exception.IllegalStateException;
import org.sang.labmanagement.exception.OperationNotPermittedException;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
	/**
	 * Handles LockedException, typically thrown when an account is locked.
	 */
	@ExceptionHandler(LockedException.class)
	public ResponseEntity<ExceptionResponse> handleException(LockedException exp){
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(
						ExceptionResponse.builder()
								.businessErrorCode(BusinessErrorCodes.ACCOUNT_LOCKED.getCode())
								.businessErrorDescription(BusinessErrorCodes.ACCOUNT_LOCKED.getDescription())
								.error(exp.getMessage())
								.timestamp(LocalDateTime.now())
								.build()
				);

	}

	/**
	 * Handles DisabledException, thrown when an account is disabled.
	 */
	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ExceptionResponse> handleException(DisabledException exp){
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(
						ExceptionResponse.builder()
								.businessErrorCode(BusinessErrorCodes.ACCOUNT_DISABLED.getCode())
								.businessErrorDescription(BusinessErrorCodes.ACCOUNT_DISABLED.getDescription())
								.error(exp.getMessage())
								.timestamp(LocalDateTime.now())
								.build()
				);

	}

	/**
	 * Handles BadCredentialsException, indicating that authentication failed due to invalid credentials.
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ExceptionResponse> handleException(BadCredentialsException exp){
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(
						ExceptionResponse.builder()
								.businessErrorCode(BusinessErrorCodes.BAD_CREDENTIALS.getCode())
								.businessErrorDescription(BusinessErrorCodes.BAD_CREDENTIALS.getDescription())
								.error(BusinessErrorCodes.BAD_CREDENTIALS.getDescription())
								.timestamp(LocalDateTime.now())
								.build()
				);

	}

	/**
	 * Handles MessagingException, which may occur during email sending operations.
	 */
	@ExceptionHandler(MessagingException.class)
	public ResponseEntity<ExceptionResponse> handleException(MessagingException exp){
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(
						ExceptionResponse.builder()
								.error(exp.getMessage())
								.timestamp(LocalDateTime.now())
								.build()
				);

	}

	/**
	 * Handles MethodArgumentNotValidException, typically thrown when validation on an argument annotated with @Valid fails.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ExceptionResponse> handleException(MethodArgumentNotValidException exp){
		Set<String> errors = new HashSet<>();
		exp.getBindingResult().getAllErrors()
				.forEach(error -> {
					var errorMessage = error.getDefaultMessage();
					errors.add(errorMessage);
				});
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(
						ExceptionResponse.builder()
								.validationErrors(errors)
								.timestamp(LocalDateTime.now())
								.build()
				);

	}

	/**
	 * Handles any other general exceptions that do not have specific handlers.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ExceptionResponse> handleException(Exception exp){
		// Log the exception
		exp.printStackTrace();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(
						ExceptionResponse.builder()
								.businessErrorDescription("Internal error, contact the admin")
								.error(exp.getMessage())
								.timestamp(LocalDateTime.now())
								.build()
				);

	}

	@ExceptionHandler(OperationNotPermittedException.class)
	public ResponseEntity<ExceptionResponse> handleException(OperationNotPermittedException exp){
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(
						ExceptionResponse.builder()
								.error(exp.getMessage())
								.timestamp(LocalDateTime.now())
								.build()
				);

	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ExceptionResponse> handleException(ResourceNotFoundException exp){
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(
						ExceptionResponse.builder()
								.error(exp.getMessage())
								.timestamp(LocalDateTime.now())
								.build()
				);

	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ExceptionResponse> handleException(IllegalStateException exp){
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(
						ExceptionResponse.builder()
								.error(exp.getMessage())
								.timestamp(LocalDateTime.now())
								.build()
				);

	}


	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ExceptionResponse> handleException(RuntimeException exp){
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(
						ExceptionResponse.builder()
								.error(exp.getMessage())
								.timestamp(LocalDateTime.now())
								.build()
				);

	}





}
