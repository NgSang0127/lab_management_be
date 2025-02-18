package org.sang.labmanagement.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserByAdminRequest {

	private String firstName;

	private String lastName;

	private String username;

	private String password;

	private String email;

	private String phoneNumber;

	private boolean enabled;

	private boolean accountLocked;

	private String role;


}
