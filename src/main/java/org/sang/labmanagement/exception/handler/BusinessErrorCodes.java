package org.sang.labmanagement.exception.handler;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BusinessErrorCodes {
	// General Errors
	NO_CODE(0, HttpStatus.NOT_IMPLEMENTED, "No code"),
	INTERNAL_SERVER_ERROR(100, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
	BAD_REQUEST(101, HttpStatus.BAD_REQUEST, "Invalid request"),
	UNAUTHORIZED(102, HttpStatus.UNAUTHORIZED, "Unauthorized request"),
	FORBIDDEN(103, HttpStatus.FORBIDDEN, "Access is denied"),
	NOT_FOUND(104, HttpStatus.NOT_FOUND, "Resource not found"),
	METHOD_NOT_ALLOWED(105, HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not allowed"),
	UNSUPPORTED_MEDIA_TYPE(106, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),
	SERVICE_UNAVAILABLE(107, HttpStatus.SERVICE_UNAVAILABLE, "Service is temporarily unavailable"),
	ILLEGAL_STATE(400,HttpStatus.BAD_REQUEST,"Action not consistent with system state"),

	// Authentication & Authorization Errors
	INVALID_VERIFICATION_CODE(400, HttpStatus.BAD_REQUEST, "The verification code is incorrect or has expired"),
	INCORRECT_CURRENT_PASSWORD(200, HttpStatus.BAD_REQUEST, "Current password is incorrect"),
	NEW_PASSWORD_DOES_NOT_MATCH(201, HttpStatus.BAD_REQUEST, "The new password does not match"),
	ACCOUNT_LOCKED(202, HttpStatus.FORBIDDEN, "User account is locked"),
	ACCOUNT_DISABLED(203, HttpStatus.FORBIDDEN, "User account is disabled"),
	BAD_CREDENTIALS(204, HttpStatus.UNAUTHORIZED, "Login and/or password is incorrect"),
	TOKEN_INVALID(205, HttpStatus.UNAUTHORIZED, "Authentication token is incorrect or has expired"),
	ACCESS_DENIED(207, HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
	TWO_FA_NOT_ENABLED(207, HttpStatus.BAD_REQUEST, "2FA is not enabled for this user."),
	INVALID_OTP(208, HttpStatus.UNAUTHORIZED, "Invalid OTP code."),

	// User Management Errors
	USER_NOT_FOUND(300, HttpStatus.NOT_FOUND, "User not found"),
	EMAIL_ALREADY_EXISTS(301, HttpStatus.BAD_REQUEST, "Email already exists"),
	USERNAME_ALREADY_EXISTS(302, HttpStatus.BAD_REQUEST, "Username already exists"),
	ACCOUNT_ALREADY_ACTIVATED(303, HttpStatus.BAD_REQUEST, "Account is already activated"),
	ACCOUNT_NOT_ACTIVATED(304, HttpStatus.UNAUTHORIZED, "Account is not activated"),

	// Validation Errors
	INVALID_EMAIL_FORMAT(400, HttpStatus.BAD_REQUEST, "Invalid email format"),
	INVALID_PHONE_NUMBER(401, HttpStatus.BAD_REQUEST, "Invalid phone number format"),
	FIELD_CANNOT_BE_EMPTY(402, HttpStatus.BAD_REQUEST, "Required field cannot be empty"),
	PASSWORD_TOO_WEAK(403, HttpStatus.BAD_REQUEST, "Password does not meet security requirements"),
	INVALID_DATE_FORMAT(404, HttpStatus.BAD_REQUEST, "Invalid date format"),

	// Resource Errors
	RESOURCE_NOT_FOUND(500, HttpStatus.NOT_FOUND, "Requested resource not found"),
	RESOURCE_ALREADY_EXISTS(501, HttpStatus.BAD_REQUEST, "Resource already exists"),
	RESOURCE_CONFLICT(502, HttpStatus.CONFLICT, "Resource conflict"),

	// File Upload Errors
	FILE_UPLOAD_FAILED(600, HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed"),
	FILE_TOO_LARGE(601, HttpStatus.BAD_REQUEST, "File size exceeds limit"),
	UNSUPPORTED_FILE_TYPE(602, HttpStatus.BAD_REQUEST, "Unsupported file type"),

	// API Errors
	API_RATE_LIMIT_EXCEEDED(700, HttpStatus.TOO_MANY_REQUESTS, "API rate limit exceeded"),
	API_KEY_MISSING(701, HttpStatus.UNAUTHORIZED, "API key is missing"),
	API_KEY_INVALID(702, HttpStatus.UNAUTHORIZED, "API key is invalid"),

	// Database Errors
	DATABASE_CONNECTION_FAILED(800, HttpStatus.INTERNAL_SERVER_ERROR, "Database connection failed"),
	DATA_INTEGRITY_VIOLATION(801, HttpStatus.CONFLICT, "Data integrity violation"),
	DUPLICATE_ENTRY(802, HttpStatus.BAD_REQUEST, "Duplicate entry detected"),

	// Payment Errors
	PAYMENT_FAILED(900, HttpStatus.PAYMENT_REQUIRED, "Payment processing failed"),
	PAYMENT_METHOD_NOT_SUPPORTED(901, HttpStatus.BAD_REQUEST, "Payment method not supported"),
	INSUFFICIENT_FUNDS(902, HttpStatus.PAYMENT_REQUIRED, "Insufficient funds"),

	// Notification Errors
	EMAIL_SEND_FAILED(1000, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send email"),
	SMS_SEND_FAILED(1001, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send SMS"),

	// Third-Party Integration Errors
	THIRD_PARTY_SERVICE_UNAVAILABLE(1100, HttpStatus.SERVICE_UNAVAILABLE, "Third-party service unavailable"),
	THIRD_PARTY_AUTHENTICATION_FAILED(1101, HttpStatus.UNAUTHORIZED, "Third-party authentication failed");

	private final int code;
	private final HttpStatus httpStatus;
	private final String description;

	BusinessErrorCodes(int code, HttpStatus httpStatus, String description) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.description = description;
	}
}
