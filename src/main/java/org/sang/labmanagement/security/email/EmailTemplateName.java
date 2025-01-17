package org.sang.labmanagement.security.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
	ACTIVATE_ACCOUNT("activate_account"),
	RESET_PASSWORD("reset_password"),
	MAINTENANCE_SCHEDULER("maintenance_scheduler");

	private final String name;

	EmailTemplateName(String name) {
		this.name = name;
	}
}
