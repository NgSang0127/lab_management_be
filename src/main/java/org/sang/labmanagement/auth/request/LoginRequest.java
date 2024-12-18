package org.sang.labmanagement.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

	@NotEmpty(message = "Username is required")
	@NotBlank(message = "Username is required")
	private String username;
	@NotEmpty(message = "Password is required")
	@NotBlank(message = "Password is required")
	@Size(min = 8,message = "Password should contains 8 characters at least")
	private String password;

}
