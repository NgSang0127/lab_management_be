package org.sang.labmanagement.asset.history;

import java.time.LocalDateTime;
import lombok.Data;
import org.sang.labmanagement.asset.AssetStatus;

@Data
public class AssetHistoryDTO {
	private Long id;
	private Long assetId;
	private String assetName;
	private String username;
	private Long userId;
	private AssetStatus previousStatus;
	private AssetStatus newStatus;
	private LocalDateTime changeDate;
	private String remarks;
}
