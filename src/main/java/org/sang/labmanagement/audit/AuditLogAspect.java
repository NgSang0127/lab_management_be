//package org.sang.labmanagement.audit;
//
//import java.time.LocalDateTime;
//import lombok.RequiredArgsConstructor;
//import org.aspectj.lang.JoinPoint;
//import org.aspectj.lang.annotation.AfterReturning;
//import org.aspectj.lang.annotation.AfterThrowing;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Pointcut;
//
//import org.sang.labmanagement.user.UserRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.MDC;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//
//@Aspect
//@Component
//@RequiredArgsConstructor
//public class AuditLogAspect {
//
//	//Dùng cho audit log db
////	private final AuditLogService auditLogService;
////
////	@Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
////	public void restController() {}
////
////	@AfterReturning(pointcut = "restController()", returning = "result")
////	public void logAfter(JoinPoint joinPoint, Object result) {
////		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
////		String username = auth != null ? auth.getName() : "Anonymous";
////
////		AuditLog auditLog = AuditLog.builder()
////				.user(null) // Bạn có thể load user từ database nếu cần
////				.action(joinPoint.getSignature().toShortString())
////				.timestamp(LocalDateTime.now())
////				.details("Executed method: " + joinPoint.getSignature().toShortString())
////				.build();
////
////		auditLogService.createAuditLog(auditLog);
////	}
//
//
//	private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
//
//	/**
//	 * Pointcut để intercept tất cả các phương thức trong các lớp có annotation @RestController
//	 */
//	@Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
//	public void restController() {
//	}
//
//	/**
//	 * Advice sau khi phương thức được thực thi thành công
//	 */
//	@AfterReturning(pointcut = "restController()", returning = "result")
//	public void logAfter(JoinPoint joinPoint, Object result) {
//		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//		String username = (auth != null) ? auth.getName() : "Anonymous";
//
//		// Đặt thông tin vào MDC
//		MDC.put("username", username);
//
//		String methodName = joinPoint.getSignature().toShortString();
//		String details = "Executed method: " + methodName;
//
//		auditLogger.info(details);
//
//		// Xóa thông tin khỏi MDC sau khi log
//		MDC.remove("username");
//	}
//
//	/**
//	 * Advice khi phương thức ném ra ngoại lệ
//	 */
//	@AfterThrowing(pointcut = "restController()", throwing = "ex")
//	public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {
//		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//		String username = (auth != null) ? auth.getName() : "Anonymous";
//
//		MDC.put("username", username);
//
//		String methodName = joinPoint.getSignature().toShortString();
//		String details = "Executed method: " + methodName + " and threw exception: " + ex.getMessage();
//
//		auditLogger.error(details);
//
//		MDC.remove("username");
//	}
//}