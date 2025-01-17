
package org.sang.labmanagement.asset;

import org.sang.labmanagement.asset.category.Category;
import org.sang.labmanagement.asset.location.Location;
import org.sang.labmanagement.user.User;
import org.springframework.stereotype.Component;

@Component
public class AssetMapper {

	public AssetDTO toDTO(Asset asset) {
		AssetDTO dto = new AssetDTO();
		dto.setId(asset.getId());
		dto.setName(asset.getName());
		dto.setDescription(asset.getDescription());
		dto.setSerialNumber(asset.getSerialNumber());
		dto.setStatus(asset.getStatus());
		dto.setImage(asset.getImage());
		dto.setPurchaseDate(asset.getPurchaseDate());
		dto.setPrice(asset.getPrice());
		if(asset.getCategory() != null) {
			dto.setCategoryId(asset.getCategory().getId());
		}
		if(asset.getLocation() != null) {
			dto.setLocationId(asset.getLocation().getId());
		}
		if(asset.getAssignedUser() != null) {
			dto.setAssignedUserId(asset.getAssignedUser().getId());
			dto.setAssignedUserName(asset.getAssignedUser().getFullName());
		}

		return dto;
	}

	public Asset toEntity(AssetDTO dto, Category category, Location location, User assignedUser) {
		return Asset.builder()
				.name(dto.getName())
				.description(dto.getDescription())
				.serialNumber(dto.getSerialNumber())
				.status(dto.getStatus())
				.image(dto.getImage())
				.purchaseDate(dto.getPurchaseDate())
				.price(dto.getPrice())
				.category(category)
				.location(location)
				.assignedUser(assignedUser)
				.build();
	}

	public AssetDTOByUser toDTOByUser(Asset asset) {
		AssetDTOByUser dto = new AssetDTOByUser();
		dto.setId(asset.getId());
		dto.setName(asset.getName());
		dto.setDescription(asset.getDescription());
		dto.setSerialNumber(asset.getSerialNumber());
		dto.setStatus(asset.getStatus());
		dto.setImage(asset.getImage());
		dto.setPurchaseDate(asset.getPurchaseDate());
		dto.setPrice(asset.getPrice());
		if(asset.getCategory() != null) {
			dto.setCategoryName(asset.getCategory().getName());
		}
		if(asset.getLocation() != null) {
			dto.setLocationName(asset.getLocation().getName());
		}

		return dto;
	}
}
