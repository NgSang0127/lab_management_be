package org.sang.labmanagement.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sang.labmanagement.user.Role;

@Getter
@Setter
@Builder
@JsonInclude(Include.NON_NULL)
public class AuthenticationResponse {

	private String message;

	private Role role;

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("refresh_token")
	private String refreshToken;

	private boolean tfaEnabled;

	private String secretImageUri;
}
