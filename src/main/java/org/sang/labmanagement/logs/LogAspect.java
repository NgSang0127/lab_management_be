package org.sang.labmanagement.logs;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

	private final LogsService logsService;


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
}



