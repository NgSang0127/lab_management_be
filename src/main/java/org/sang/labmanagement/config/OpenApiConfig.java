package org.sang.labmanagement.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
		info = @Info(
				contact = @Contact(
						name = "Sang",
						email = "nsang@gmail.com",
						url = "https://github.com/NgSang0127"
				),
				description = "OpenAPI documentation for Spring Security with Cookie-based auth",
				title = "OpenAPI Spec - Cookie Auth",
				version = "1.0",
				license = @License(
						name = "Licence name",
						url = "https://some-url.com"
				),
				termsOfService = "Terms of service"
		),
		servers = {
				@Server(
						description = "Local ENV",
						url = "http://localhost:8080/api/v1"
				),
				@Server(
						description = "PROD ENV",
						url = ""
				)
		},
		security = {
				@SecurityRequirement(
						name = "cookieAuth"
				)
		}
)
@SecurityScheme(
		name = "cookieAuth",
		type = SecuritySchemeType.APIKEY,
		in = SecuritySchemeIn.COOKIE,
		paramName = "access_token", // tên cookie của bạn, ví dụ: SESSIONID, JWT, authToken,...
		description = "JWT stored in cookie named 'access_token'"
)
public class OpenApiConfig {
}

