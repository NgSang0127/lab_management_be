package org.sang.labmanagement.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sang.labmanagement.user.Role;

@Getter
@Setter
public class RegistrationRequest {

	@NotEmpty(message = "Firstname is required")
	@NotBlank(message = "Firstname is required")
	private String firstName;

	@NotEmpty(message = "Lastname is required")
	@NotBlank(message = "Lastname is required")
	private String lastName;


	@Email(message = "Email is not formatted")
	@NotEmpty(message = "Firstname is required")
	@NotBlank(message = "Firstname is required")
	private String email;

	private String phoneNumber;

	@NotBlank(message = "Username is required")
	@NotEmpty(message = "Username is required")
	private String username;

	@NotEmpty(message = "Password is required")
	@NotBlank(message = "Password is required")
	@Size(min = 8,message = "Password should contains 8 characters at least")
	private String password;


}
