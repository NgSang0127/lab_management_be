package org.sang.labmanagement.asset;

import java.util.Set;
import java.util.stream.Collectors;
import org.sang.labmanagement.asset.category.Category;
import org.sang.labmanagement.asset.configuration.AssetConfigDTO;
import org.sang.labmanagement.asset.configuration.AssetConfiguration;
import org.sang.labmanagement.asset.location.Location;
import org.sang.labmanagement.room.Room;
import org.sang.labmanagement.user.User;
import org.springframework.stereotype.Component;

@Component
public class AssetMapper {

	public AssetDTO toDTO(Asset asset) {
		AssetDTO dto = new AssetDTO();
		dto.setId(asset.getId());
		dto.setName(asset.getName());
		dto.setDescription(asset.getDescription());
		dto.setStatus(asset.getStatus());
		dto.setImage(asset.getImage());
		dto.setPurchaseDate(asset.getPurchaseDate());
		dto.setPrice(asset.getPrice());
		dto.setOperationStartDate(asset.getOperationStartDate());
		dto.setOperationTime(asset.getOperationTime());
		dto.setOperationYear(asset.getOperationYear());
		dto.setCategoryId(asset.getCategory() != null ? asset.getCategory().getId() : null);
		dto.setLocationId(asset.getLocation() != null ? asset.getLocation().getId() : null);
		dto.setRoomId(asset.getRoom() != null ? asset.getRoom().getId() : null);
		dto.setAssignedUserId(asset.getAssignedUser() != null ? asset.getAssignedUser().getId() : null);
		dto.setAssignedUserName(asset.getAssignedUser() != null ? asset.getAssignedUser().getFullName() : null);
		dto.setQuantity(asset.getQuantity());
		dto.setWarranty(asset.getWarranty());
		dto.setLifeSpan(asset.getLifeSpan());
		// Ánh xạ configurations, bao gồm id
		Set<AssetConfigDTO> configDTOs = asset.getConfigurations().stream()
				.map(config -> new AssetConfigDTO(config.getSpecKey(), config.getSpecValue()))
				.collect(Collectors.toSet());
		dto.setConfigurations(configDTOs);
		return dto;
	}

	public Asset toEntity(AssetDTO dto, Category category, Location location, User assignedUser, Room room) {
		Asset asset = Asset.builder()
				.name(dto.getName())
				.description(dto.getDescription())
				.status(dto.getStatus())
				.image(dto.getImage())
				.purchaseDate(dto.getPurchaseDate())
				.price(dto.getPrice())
				.category(category)
				.location(location)
				.assignedUser(assignedUser)
				.quantity(dto.getQuantity())
				.room(room)
				.warranty(dto.getWarranty())
				.operationYear(dto.getOperationYear())
				.operationTime(dto.getOperationTime())
				.lifeSpan(dto.getLifeSpan())
				.build();
		if (dto.getConfigurations() != null) {
			dto.getConfigurations().forEach(configDTO -> {
				AssetConfiguration config = new AssetConfiguration();
				config.setSpecKey(configDTO.getSpecKey());
				config.setSpecValue(configDTO.getSpecValue());
				config.setAsset(asset);
				asset.getConfigurations().add(config);
			});
		}
		return asset;
	}

	public AssetDTOByUser toDTOByUser(Asset asset) {
		AssetDTOByUser dto = new AssetDTOByUser();
		dto.setId(asset.getId());
		dto.setName(asset.getName());
		dto.setDescription(asset.getDescription());
		dto.setStatus(asset.getStatus());
		dto.setImage(asset.getImage());
		dto.setPurchaseDate(asset.getPurchaseDate());
		dto.setPrice(asset.getPrice());
		dto.setCategoryName(asset.getCategory() != null ? asset.getCategory().getName() : null);
		dto.setLocationName(asset.getLocation() != null ? asset.getLocation().getName() : null);
		dto.setRoomName(asset.getRoom() != null ? asset.getRoom().getName() : null);
		dto.setOperationYear(asset.getOperationYear());
		dto.setOperationStartDate(asset.getOperationStartDate());
		dto.setOperationTime(asset.getOperationTime());
		dto.setLifeSpan(asset.getLifeSpan());
		return dto;
	}
}