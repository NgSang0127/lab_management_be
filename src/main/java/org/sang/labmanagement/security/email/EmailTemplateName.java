package org.sang.labmanagement.security.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
	ACTIVATE_ACCOUNT("activate_account"),
	RESET_PASSWORD("reset_password"),
	MAINTENANCE_SCHEDULER("maintenance_scheduler"),
	TWO_FACTOR_AUTHENTICATION("two_factor_authentication");

	private final String name;

	EmailTemplateName(String name) {
		this.name = name;
	}
}
