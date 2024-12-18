package org.sang.labmanagement.config;


import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.activity.UserActivityInterceptor;
import org.sang.labmanagement.logs.LogsInterceptor;
import org.sang.labmanagement.logs.LogsService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig  implements WebMvcConfigurer {
	private final LogsService logService;
	private final UserActivityInterceptor userActivityInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		System.out.println("Adding Logs Interceptor...");

		registry.addInterceptor(new LogsInterceptor(logService))
				.addPathPatterns("/api/v1/timetable/course-details");

		registry.addInterceptor(userActivityInterceptor)
				.excludePathPatterns("/api/v1/auth/**");
	}
}