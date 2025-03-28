package org.sang.labmanagement.exception;

public class AccountAlreadyActivatedException extends  RuntimeException{
	public AccountAlreadyActivatedException(String message) {
		super(message);
	}
}
