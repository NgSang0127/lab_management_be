package org.sang.labmanagement.auth.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateInformationUser {
	private String image;
	private String firstName;
	private String lastName;
	private String email;
	private String phoneNumber;
	private String username;

}
