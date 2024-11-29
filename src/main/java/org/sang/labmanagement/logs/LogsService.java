package org.sang.labmanagement.logs;

import java.time.LocalDateTime;
import java.util.List;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.security.core.Authentication;

public interface LogsService {
	void saveLog(String endpoint, LocalDateTime timestamp, String action, Authentication connectedUser,
			String codeCourse,
			String NH, String TH,String timetableName,
			String ipAddress, String userAgent);

	PageResponse<Logs> getLogsBetweenDates(LocalDateTime startDate, LocalDateTime endDate,int page,int size);

	List<Logs> getLogsByAction(String action);

	List<Logs> getLogsByCourse(Long courseId);

	List<Logs> getLogsByUser(Long userId);
}
