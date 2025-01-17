package org.sang.labmanagement.asset.asset_history;

import org.sang.labmanagement.asset.Asset;
import org.sang.labmanagement.user.User;
import org.springframework.stereotype.Component;

@Component
public class AssetHistoryMapper {
	public AssetHistoryDTO toDTO(AssetHistory assetHistory){
		AssetHistoryDTO dto=new AssetHistoryDTO();
		dto.setId(assetHistory.getId());
		dto.setAssetId(assetHistory.getAsset().getId());
		dto.setUserId(assetHistory.getUser().getId());
		dto.setPreviousStatus(assetHistory.getPreviousStatus());
		dto.setNewStatus(assetHistory.getNewStatus());
		dto.setChangeDate(assetHistory.getChangeDate());
		dto.setRemarks(assetHistory.getRemarks());
		return dto;

	}

	public AssetHistory toEntity(AssetHistoryDTO dto, Asset asset, User user){
		return AssetHistory.builder()
				.asset(asset)
				.user(user)
				.previousStatus(dto.getPreviousStatus())
				.newStatus(dto.getNewStatus())
				.changeDate(dto.getChangeDate())
				.remarks(dto.getRemarks())
				.build();
	}
}
