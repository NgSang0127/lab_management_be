package org.sang.labmanagement.asset.maintenance;

import org.sang.labmanagement.asset.Asset;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceMapper {
	public MaintenanceDTO toDTO(Maintenance maintenance){
		MaintenanceDTO dto=new MaintenanceDTO();
		dto.setAssetId(maintenance.getAsset().getId());
		dto.setScheduledDate(maintenance.getScheduleDate());
		dto.setStatus(maintenance.getStatus());
		dto.setRemarks(maintenance.getRemarks());
		dto.setAssetName(maintenance.getAsset().getName());
		return dto;

	}

	public Maintenance toEntity(MaintenanceDTO dto, Asset asset){
		return Maintenance.builder()
				.asset(asset)
				.scheduleDate(dto.getScheduledDate())
				.status(dto.getStatus())
				.remarks(dto.getRemarks())
				.createDate(dto.getCreateDate())
				.lastModifiedDate(dto.getLastModifiedDate())
				.build();
	}
}
