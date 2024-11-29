package org.sang.labmanagement.logs;



import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/logs")
public class LogsController {
	private final LogsService logService;

	@GetMapping("/between")
	public ResponseEntity<PageResponse<Logs>> getLogsBetween(
			@RequestParam LocalDate startDate,
			@RequestParam LocalDate endDate,
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
	) {
		// Chuyển LocalDate thành LocalDateTime (startDate -> bắt đầu ngày, endDate -> kết thúc ngày)
		LocalDateTime startDateTime = startDate.atStartOfDay();  // Bắt đầu ngày
		LocalDateTime endDateTime = endDate.atTime(23, 59, 59);  // Kết thúc ngày (23:59:59)

		return ResponseEntity.ok(logService.getLogsBetweenDates(startDateTime, endDateTime,page,size));
	}




	@GetMapping("/action")
	public List<Logs> getLogsByAction(@RequestParam String action){
		return logService.getLogsByAction(action);
	}

	@GetMapping("/course")
	public List<Logs> getLogsByCourse(
			@RequestParam Long courseId
	){
		return logService.getLogsByCourse(courseId);
	}

	@GetMapping("/user")
	public List<Logs> getLogsByUser(@RequestParam Long userId){
		return logService.getLogsByUser(userId);
	}

}
