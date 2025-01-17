package org.sang.labmanagement.audit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
	private final AuditLogRepository auditLogRepository;

	public PageResponse<AuditLog> getAllAuditLogs(int page ,int size){
		Pageable pageable= PageRequest.of(page,size);
		Page<AuditLog>auditLogPage=auditLogRepository.findAll(pageable);
		return PageResponse.<AuditLog>builder()
				.content(auditLogPage.getContent())
				.number(auditLogPage.getNumber())
				.size(auditLogPage.getSize())
				.totalElements(auditLogPage.getTotalElements())
				.totalPages(auditLogPage.getTotalPages())
				.first(auditLogPage.isFirst())
				.last(auditLogPage.isLast())
				.build();
	}

	public AuditLog createAuditLog(AuditLog auditLog){
		return auditLogRepository.save(auditLog);
	}
}
