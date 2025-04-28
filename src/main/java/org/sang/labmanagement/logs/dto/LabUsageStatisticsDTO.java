package org.sang.labmanagement.logs.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LabUsageStatisticsDTO {
	private Long roomId;
	private String roomName;
	private double usagePercentage; // Tần suất sử dụng (%)
}
