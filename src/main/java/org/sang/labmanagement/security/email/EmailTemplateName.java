package org.sang.labmanagement.security.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
	ACTIVATE_ACCOUNT("activate_account"),
	RESET_PASSWORD("reset_password"),
	MAINTENANCE_SCHEDULER("maintenance_scheduler"),
	TWO_FACTOR_AUTHENTICATION("two_factor_authentication"),
	OVERDUE_BORROWING("overdue_borrowing"),
	CONTACT_USER("contact_form"),
	CONTACT_ADMIN("contact_admin");

	private final String name;

	EmailTemplateName(String name) {
		this.name = name;
	}
}
