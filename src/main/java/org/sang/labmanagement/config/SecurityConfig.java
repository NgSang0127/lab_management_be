package org.sang.labmanagement.config;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.security.jwt.JwtFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

	private final JwtFilter jwtFilter;
	private final AuthenticationProvider authenticationProvider;
	private final LogoutHandler logoutHandler;
	private final CorsFilter corsFilter;

	@Value("${application.cors.origins}")
	private String corsOrigins;


	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/api/v1/auth/**",
								"/v2/api-docs",
								"/v3/api-docs",
								"/v3/api-docs/**",
								"/swagger-resources",
								"/swagger-resources/**",
								"/configuration/ui",
								"/configuration/security",
								"/swagger-ui/**",
								"/webjars/**",
								"/swagger-ui.html"
						).permitAll()
						.requestMatchers("/api/v1/lesson-time").permitAll() // Allow public access to lesson times
						.requestMatchers("/api/v1/timetable/weeks-range").permitAll() // Allow public access to weeks range
						.requestMatchers("/api/v1/timetable/by-week").permitAll()
						.requestMatchers("/api/v1/timetable/course-details").permitAll()
						.requestMatchers("/api/v1/timetable/semester").permitAll()
						.requestMatchers("/api/v1/admin/rooms").permitAll()
						.requestMatchers("/api/v1/admin/semester").permitAll()
						.requestMatchers("/ws/**", "/chat/**").permitAll()
						.requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN","OWNER","CO_OWNER")
						.requestMatchers("/teacher/**").hasRole("TEACHER")
						.requestMatchers("/student/**").hasRole("STUDENT")
						.requestMatchers("/api/v1/**").authenticated()
						.anyRequest().permitAll()
				)
				.authenticationProvider(authenticationProvider)
				.addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
				.logout(logout ->
						logout.logoutUrl("/api/v1/auth/logout")
								.addLogoutHandler(logoutHandler)
								.logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext())
				);
		return http.build();
	}

	private CorsConfigurationSource corsConfigurationSource() {
		return request -> {
			CorsConfiguration corsConfig = new CorsConfiguration();
			List<String> allowedOrigins = Arrays.asList(corsOrigins.split(","));
			corsConfig.setAllowedOriginPatterns(allowedOrigins);

			corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH"));

			corsConfig.setAllowedHeaders(Arrays.asList(
					"Authorization",
					"X-Refresh-Token",
					"Content-Type",
					"Accept-Language",
					"Accept",
					"Origin",
					"X-Requested-With",
					"Access-Control-Request-Method",
					"Access-Control-Request-Headers"
			));

			corsConfig.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

			corsConfig.setAllowCredentials(true);

			corsConfig.setMaxAge(3600L);
			return corsConfig;
		};
	}

}