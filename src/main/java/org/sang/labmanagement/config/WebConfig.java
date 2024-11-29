package org.sang.labmanagement.config;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.logs.LogsInterceptor;
import org.sang.labmanagement.logs.LogsService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig  implements WebMvcConfigurer {
	private final LogsService logService;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		System.out.println("Adding Logs Interceptor...");
		registry.addInterceptor(new LogsInterceptor(logService))
				.addPathPatterns("/api/v1/timetable/course-details/**");
	}
}