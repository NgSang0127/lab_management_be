package org.sang.labmanagement.asset;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sang.labmanagement.asset.borrow.AssetBorrowing;
import org.sang.labmanagement.asset.borrow.AssetBorrowingDTO;
import org.sang.labmanagement.asset.borrow.AssetBorrowingRepository;
import org.sang.labmanagement.asset.borrow.BorrowingStatus;
import org.sang.labmanagement.asset.history.AssetHistory;
import org.sang.labmanagement.asset.history.AssetHistoryDTO;
import org.sang.labmanagement.asset.history.AssetHistoryMapper;
import org.sang.labmanagement.asset.history.AssetHistoryRepository;
import org.sang.labmanagement.asset.category.Category;
import org.sang.labmanagement.asset.category.CategoryService;
import org.sang.labmanagement.asset.configuration.AssetConfigDTO;
import org.sang.labmanagement.asset.configuration.AssetConfiguration;
import org.sang.labmanagement.asset.configuration.AssetConfigurationRepository;
import org.sang.labmanagement.asset.history.AssetHistoryService;
import org.sang.labmanagement.asset.location.Location;
import org.sang.labmanagement.asset.location.LocationService;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.sang.labmanagement.room.Room;
import org.sang.labmanagement.room.RoomService;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserDetailsServiceImplement;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetService {

	private final AssetRepository assetRepository;
	private final CategoryService categoryService;
	private final LocationService locationService;
	private final AssetMapper assetMapper;
	private final UserDetailsServiceImplement userService;
	private final AssetHistoryRepository assetHistoryRepository;
	private final RoomService roomService;
	private final AssetHistoryService assetHistoryService;
	private final AssetConfigurationRepository assetConfigurationRepository;


	public PageResponse<AssetDTO> getAllAssets(Pageable pageable, String keyword, String roomName, String statuses,
			String categoryIds) {
		Specification<Asset> spec = AssetSpecification.getAssetsByKeywordAndRoom(keyword, roomName, statuses,
				categoryIds);
		Page<Asset> assetPage = assetRepository.findAll(spec, pageable);

		List<Asset> assetList = assetPage.getContent();

		Map<String, Integer> counterMap = new HashMap<>();

		List<AssetDTO> assetDTOList = assetList.stream().map(asset -> {
			AssetDTO dto = assetMapper.toDTO(asset);
			String roomNameStr = asset.getRoom() != null ? asset.getRoom().getName() : "Unknown";
			String key = asset.getName() + "_" + roomNameStr;

			// Tăng số lượng theo key
			int count = counterMap.getOrDefault(key, 0) + 1;
			counterMap.put(key, count);

			dto.setDescription(asset.getName() + " _ " + roomNameStr + " #" + count);
			return dto;
		}).collect(Collectors.toList());

		return PageResponse.<AssetDTO>builder()
				.content(assetDTOList)
				.number(assetPage.getNumber())
				.size(assetPage.getSize())
				.totalElements(assetPage.getTotalElements())
				.totalPages(assetPage.getTotalPages())
				.first(assetPage.isFirst())
				.last(assetPage.isLast())
				.build();
	}

	public AssetDTO createAsset(AssetDTO assetDTO) {
		Category category = null;
		if (assetDTO.getCategoryId() != null) {
			category = categoryService.getCategoryById(assetDTO.getCategoryId());
		}

		Location location = null;
		if (assetDTO.getLocationId() != null) {
			location = locationService.getLocationById(assetDTO.getLocationId());
		}

		User assignedUser = null;
		if (assetDTO.getAssignedUserId() != null) {
			assignedUser = userService.getUserById(assetDTO.getAssignedUserId());
		}

		Room room = null;
		if (assetDTO.getRoomId() != null) {
			room = roomService.getRoomById(assetDTO.getRoomId());
		}

		Asset asset = assetMapper.toEntity(assetDTO, category, location, assignedUser, room);
		asset.setPurchaseDate(LocalDateTime.now());
		asset.setOperationStartDate(assetDTO.getOperationStartDate() != null ? assetDTO.getOperationStartDate() :
				LocalDate.now());
		asset.setOperationTime(
				assetDTO.getOperationTime() != null ? assetDTO.getOperationTime() : OperationTime.FULL_DAY);
		asset.setConfigurations(assetDTO.getConfigurations().stream()
				.map(configDTO -> {
					AssetConfiguration config = new AssetConfiguration();
					config.setSpecKey(configDTO.getSpecKey());
					config.setSpecValue(configDTO.getSpecValue());
					config.setAsset(asset);
					return config;
				}).collect(Collectors.toSet())
		);

		AssetStatus previousStatus = asset.getStatus();
		Asset savedAsset = assetRepository.save(asset);

		if (assignedUser != null) {
			asset.setAssignedUser(assignedUser);
			asset.setStatus(AssetStatus.IN_USE);
			assetRepository.save(asset);

			AssetHistory assetHistory = AssetHistory.builder()
					.asset(asset)
					.user(assignedUser)
					.previousStatus(previousStatus)
					.newStatus(asset.getStatus())
					.changeDate(LocalDateTime.now())
					.remarks("Assigned to user " + assignedUser.getFullName())
					.build();
			assetHistoryRepository.save(assetHistory);
		}
		return assetMapper.toDTO(savedAsset);
	}

	public AssetDTO duplicateAsset(Long assetId) {
		Asset original = assetRepository.findById(assetId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

		Asset copy = new Asset();
		copy.setName(original.getName() + " (Copy)");
		copy.setDescription(original.getDescription());
		copy.setImage(original.getImage());
		copy.setStatus(AssetStatus.AVAILABLE);
		copy.setPurchaseDate(LocalDateTime.now());
		copy.setPrice(original.getPrice());
		copy.setCategory(original.getCategory());
		copy.setLocation(original.getLocation());
		copy.setRoom(original.getRoom());
		copy.setQuantity(original.getQuantity());
		copy.setWarranty(original.getWarranty());
		copy.setOperationYear(original.getOperationYear());
		copy.setOperationStartDate(original.getOperationStartDate() != null ?
				original.getOperationStartDate() : LocalDate.now());
		copy.setOperationTime(original.getOperationTime() != null ?
				original.getOperationTime() : OperationTime.FULL_DAY);
		copy.setLifeSpan(original.getLifeSpan());

		// Copy configurations
		Set<AssetConfiguration> configs = original.getConfigurations().stream()
				.map(config -> {
					AssetConfiguration newConfig = new AssetConfiguration();
					newConfig.setSpecKey(config.getSpecKey());
					newConfig.setSpecValue(config.getSpecValue());
					newConfig.setAsset(copy);
					return newConfig;
				}).collect(Collectors.toSet());

		copy.setConfigurations(configs);

		copy.setAssignedUser(null);

		Asset savedCopy = assetRepository.save(copy);
		return assetMapper.toDTO(savedCopy);
	}

	public AssetDTO getAssetById(Long id) {
		Asset asset = assetRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
		return assetMapper.toDTO(asset);
	}

	public AssetDTO updateAsset(Long id, AssetDTO assetDTO) {
		Asset existingAsset = assetRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));

		existingAsset.setName(assetDTO.getName());
		existingAsset.setImage(assetDTO.getImage());
		existingAsset.setQuantity(assetDTO.getQuantity());
		existingAsset.setDescription(assetDTO.getDescription());
		existingAsset.setStatus(assetDTO.getStatus());
		existingAsset.setPurchaseDate(assetDTO.getPurchaseDate());
		existingAsset.setPrice(assetDTO.getPrice());
		existingAsset.setOperationYear(assetDTO.getOperationYear());
		existingAsset.setOperationStartDate(assetDTO.getOperationStartDate());
		existingAsset.setLifeSpan(assetDTO.getLifeSpan());
		existingAsset.setWarranty(assetDTO.getWarranty());
		existingAsset.setOperationTime(assetDTO.getOperationTime());

		Category category =
				assetDTO.getCategoryId() != null ? categoryService.getCategoryById(assetDTO.getCategoryId()) : null;
		existingAsset.setCategory(category);

		Location location =
				assetDTO.getLocationId() != null ? locationService.getLocationById(assetDTO.getLocationId()) : null;
		existingAsset.setLocation(location);

		User currentAssignedUser = existingAsset.getAssignedUser();
		Long newAssignedUserId = assetDTO.getAssignedUserId();
		User newAssignedUser = (newAssignedUserId != null && newAssignedUserId != 0)
				? userService.getUserById(newAssignedUserId)
				: null;

		boolean isAssignmentChanged =
				(currentAssignedUser == null && newAssignedUser != null) ||
						(currentAssignedUser != null && (newAssignedUser == null || !currentAssignedUser.getId().equals(newAssignedUserId)));

		if (isAssignmentChanged) {
			if (newAssignedUser != null) {
				return assetHistoryService.assignAssetToUser(id, newAssignedUserId);
			} else {
				return assetHistoryService.unassignAsset(id);
			}
		}




		Room room = assetDTO.getRoomId() != null ? roomService.getRoomById(assetDTO.getRoomId()) : null;
		existingAsset.setRoom(room);

		Set<AssetConfiguration> existingConfigs = existingAsset.getConfigurations();
		Set<AssetConfigDTO> newConfigs = assetDTO.getConfigurations();

		if (newConfigs == null || newConfigs.isEmpty()) {
			assetConfigurationRepository.deleteAll(existingConfigs);
			existingConfigs.clear();
		} else {
			// Chuyển newConfigs thành Map để dễ so sánh
			Map<String, AssetConfigDTO> newConfigMap = newConfigs.stream()
					.collect(Collectors.toMap(
							AssetConfigDTO::getSpecKey,
							config -> config,
							(oldValue, newValue) -> newValue
					));

			// Tìm các config cần xóa (không còn trong DTO)
			Set<AssetConfiguration> configsToRemove = existingConfigs.stream()
					.filter(config -> !newConfigMap.containsKey(config.getSpecKey()))
					.collect(Collectors.toSet());

			if (!configsToRemove.isEmpty()) {
				configsToRemove.forEach(
						config -> System.out.println("Deleting config with specKey: " + config.getSpecKey()));
				assetConfigurationRepository.deleteAll(configsToRemove);
				existingConfigs.removeAll(configsToRemove);
			}

			for (AssetConfigDTO configDTO : newConfigs) {
				String specKey = configDTO.getSpecKey();
				Optional<AssetConfiguration> existingConfigOpt = existingConfigs.stream()
						.filter(config -> config.getSpecKey().equals(specKey))
						.findFirst();

				if (existingConfigOpt.isPresent()) {
					// Cập nhật specValue nếu specKey đã tồn tại
					AssetConfiguration config = existingConfigOpt.get();
					config.setSpecValue(configDTO.getSpecValue());
				} else {
					AssetConfiguration newConfig = new AssetConfiguration();
					newConfig.setSpecKey(specKey);
					newConfig.setSpecValue(configDTO.getSpecValue());
					newConfig.setAsset(existingAsset);
					existingConfigs.add(newConfig);
				}
			}
		}

		Asset updatedAsset = assetRepository.save(existingAsset);
		return assetMapper.toDTO(updatedAsset);
	}

	public void deleteAsset(Long id) {
		Asset asset = assetRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
		assetRepository.delete(asset);
	}


	public PageResponse<AssetDTOByUser> getAssetsByUserId(
			Long userId, Pageable pageable, String keyword, String roomName, String statuses, String categoryIds) {

		Specification<Asset> baseSpec = (root, query, cb) ->
				cb.equal(root.get("assignedUser").get("id"), userId);

		Specification<Asset> filterSpec = AssetSpecification.getAssetsByKeywordAndRoom(
				keyword, roomName, statuses, categoryIds
		);

		Specification<Asset> spec = baseSpec.and(filterSpec);

		Page<Asset> assets = assetRepository.findAll(spec, pageable);


		List<AssetDTOByUser> assetDTOList = assets.getContent().stream()
				.map(assetMapper::toDTOByUser)
				.collect(Collectors.toList());

		return PageResponse.<AssetDTOByUser>builder()
				.content(assetDTOList)
				.number(assets.getNumber())
				.size(assets.getSize())
				.totalElements(assets.getTotalElements())
				.totalPages(assets.getTotalPages())
				.first(assets.isFirst())
				.last(assets.isLast())
				.build();
	}



}
