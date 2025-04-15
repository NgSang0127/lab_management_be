package org.sang.labmanagement.security.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactFormDto {
	@NotBlank(message = "Name is required")
	private String name;

	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	private String email;

	@NotBlank(message = "Message is required")
	private String message;

	private String subject; // Tùy chọn
	private EmailTemplateName emailTemplate; // Tùy chọn
	private String locale; // Tùy chọn
}
