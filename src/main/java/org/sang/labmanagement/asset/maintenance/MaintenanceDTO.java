package org.sang.labmanagement.asset.maintenance;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MaintenanceDTO {
	private Long assetId;
	private String assetName;
	private LocalDateTime scheduledDate;
	private MaintenanceStatus status;
	private String remarks;
	private LocalDateTime createDate;
	private LocalDateTime lastModifiedDate;
}
