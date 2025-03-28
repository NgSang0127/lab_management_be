package org.sang.labmanagement.logs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LogAspect {

	private final LogsService logsService;
	private final HttpServletRequest request;

	private final ObjectMapper objectMapper=new ObjectMapper();


	@Pointcut("@annotation(org.sang.labmanagement.utils.TrackUserActivity) || @within(org.sang.labmanagement.utils"
			+ ".TrackUserActivity)")
	public void trackUserActivityPointcut() {
	}



	@Before("trackUserActivityPointcut()")
	public void logBefore(JoinPoint joinPoint) {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if(attributes == null){
			return;
		}
		HttpServletRequest request = attributes.getRequest();

		String endpoint = request.getRequestURI();
		String ipAddress = request.getHeader("X-Forwarded-For");
		if (ipAddress == null || ipAddress.isEmpty()) {
			ipAddress = request.getRemoteAddr();
		}

		String userAgent = request.getHeader("User-Agent");

		String timestampParam = request.getParameter("timestamp");
		LocalDateTime timestamp;
		if (timestampParam != null && !timestampParam.isEmpty()) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			try {
				timestamp = LocalDateTime.parse(timestampParam, formatter);
			} catch (Exception e) {
				timestamp = LocalDateTime.now();
			}
		} else {
			timestamp = LocalDateTime.now();
		}

		Authentication connectedUser = (Authentication) request.getUserPrincipal();
		if (connectedUser == null) {
			connectedUser = new AnonymousAuthenticationToken("anonymous", "anonymous", AuthorityUtils.NO_AUTHORITIES);
		}

		// Lấy thông tin action từ endpoint
		String action = getActionFromEndpoint(endpoint, request);

		// Lấy các tham số khác
		String courseId = request.getParameter("courseId");
		String NH = request.getParameter("NH");
		String TH = request.getParameter("TH");
		String courseName = request.getParameter("timetableName");

		// Kiểm tra các tham số trước khi ghi log
		if (endpoint != null && timestamp != null && connectedUser != null && action != null) {
			logsService.saveLog(endpoint, timestamp, action, connectedUser, courseId, NH, TH, courseName,
					ipAddress, userAgent);
		}

	}

	/**
	 * Method để xác định action dựa trên endpoint và request
	 */
	private String getActionFromEndpoint(String endpoint, HttpServletRequest request) {
		String action = "";

		if (endpoint.contains("/create")) {
			action = "create timetable"; // Tạo thời khóa biểu
		} else if (endpoint.contains("/cancel")) {
			action = "cancel timetable"; // Hủy thời khóa biểu
		} else if (endpoint.contains("/by-week")) {
			action = "view timetable by week"; // Xem thời khóa biểu theo tuần
		} else if (endpoint.contains("/by-date")) {
			action = "view timetable by date"; // Xem thời khóa biểu theo ngày
		} else if (endpoint.contains("/import")) {
			action = "import timetable data"; // Nhập dữ liệu thời khóa biểu từ file
		} else if (endpoint.contains("/course-details")) {
			// Lấy ID môn học từ request nếu có
			String courseId = request.getParameter("courseId");
			if (courseId != null && !courseId.isEmpty()) {
				action = "view timetable by course (Course ID: " + courseId + ")";
			} else {
				action = "view timetable by course (Course ID: unknown)";
			}
		}
		return action;
	}

	//Log
	@Around("@annotation(org.sang.labmanagement.utils.LogExecution)")
	public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
		long startTime = System.currentTimeMillis();
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		String className = signature.getDeclaringType().getSimpleName();
		String methodName = signature.getName();
		Object[] args = joinPoint.getArgs();


		logStart(className, methodName, args);

		try {
			Object result = joinPoint.proceed();
			long duration = System.currentTimeMillis() - startTime;

			logSuccess( className, methodName, duration, result);
			return result;
		} catch (Exception e) {
			long duration = System.currentTimeMillis() - startTime;
			logFailure(methodName, duration, e);
			throw e;
		} finally {
			MDC.clear();
		}
	}

	private void logStart(String className, String methodName, Object[] args) {
		String requestURI = request.getRequestURI();
		String clientIP = request.getRemoteAddr();
		String userAgent = request.getHeader("User-Agent");
		String username = getCurrentUsername();

		// Log dạng JSON để dễ dàng phân tích trên ELK
		Map<String, Object> logMap = new HashMap<>();
		logMap.put("status", "START");
		logMap.put("requestURI", requestURI);
		logMap.put("class", className);
		logMap.put("method", methodName);
		logMap.put("params", Arrays.toString(args));
		logMap.put("ip", clientIP);
		logMap.put("userAgent", userAgent);
		logMap.put("user", username);

		log.info("{}",logMap);
	}

	private void logSuccess( String className, String methodName, long duration, Object result) {
		Map<String, Object> logMap = new HashMap<>();
		logMap.put("status", "SUCCESS");
		logMap.put("method", methodName);
		logMap.put("duration_ms", duration);
		logMap.put("result", result);

		log.info(writeJson(logMap));

		// Nếu thời gian chạy > 2 giây, cảnh báo API chậm
		if (duration > 2000) {
			log.warn("⚠ [SLOW REQUEST] {}.{}() took {}ms", className, methodName, duration);
		}
	}

	private void logFailure( String methodName, long duration, Exception e) {
		Map<String, Object> logMap = new HashMap<>();
		logMap.put("status", "FAILED");
		logMap.put("method", methodName);
		logMap.put("duration_ms", duration);
		logMap.put("error", e.getMessage());

		log.error(writeJson(logMap));
	}

	private String getCurrentUsername() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof UserDetails) {
			return ((UserDetails) auth.getPrincipal()).getUsername();
		}
		return "Anonymous";
	}

	private String writeJson(Map<String, Object> logMap) {
		try {
			return objectMapper.writeValueAsString(logMap);
		} catch (Exception e) {
			return logMap.toString(); // Nếu lỗi, log ở dạng text
		}
	}

}



