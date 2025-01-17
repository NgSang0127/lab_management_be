package org.sang.labmanagement.config;


import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.activity.UserActivityInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig  implements WebMvcConfigurer {

	private final UserActivityInterceptor userActivityInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		System.out.println("Adding Logs Interceptor...");

		registry.addInterceptor(userActivityInterceptor)
				.excludePathPatterns("/api/v1/auth/**");
	}
}