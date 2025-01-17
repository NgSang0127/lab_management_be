package org.sang.labmanagement.audit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
	private final AuditLogService auditLogService;

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping
	public ResponseEntity<PageResponse<AuditLog>> getAllAuditLogs(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
	) {
		return ResponseEntity.ok(auditLogService.getAllAuditLogs(page,size));
	}
}
