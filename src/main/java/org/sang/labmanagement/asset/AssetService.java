package org.sang.labmanagement.asset;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.asset.asset_history.AssetHistory;
import org.sang.labmanagement.asset.asset_history.AssetHistoryDTO;
import org.sang.labmanagement.asset.asset_history.AssetHistoryMapper;
import org.sang.labmanagement.asset.asset_history.AssetHistoryRepository;
import org.sang.labmanagement.asset.category.Category;
import org.sang.labmanagement.asset.category.CategoryService;
import org.sang.labmanagement.asset.location.Location;
import org.sang.labmanagement.asset.location.LocationService;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserDetailsServiceImplement;
import org.sang.labmanagement.user.UserRepository;
import org.sang.labmanagement.user.UserSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
	private final UserRepository userRepository;
	private final AssetHistoryRepository assetHistoryRepository;
	private final AssetHistoryMapper assetHistoryMapper;

	public PageResponse<AssetDTO> getAllAssets(int page,int size,String keyword,String status){
		Pageable pageable= PageRequest.of(page,size);
		Specification<Asset> spec = AssetSpecification.getAssetsByKeywordAndStatus(keyword, status);
		Page<Asset> assets=assetRepository.findAll(spec,pageable);

		List<AssetDTO> assetDTOList = assets.getContent().stream()
				.map(assetMapper::toDTO)
				.collect(Collectors.toList());

		return PageResponse.<AssetDTO>builder()
				.content(assetDTOList)
				.number(assets.getNumber())
				.size(assets.getSize())
				.totalElements(assets.getTotalElements())
				.totalPages(assets.getTotalPages())
				.first(assets.isFirst())
				.last(assets.isLast())
				.build();
	}

	public AssetDTO createAsset(AssetDTO assetDTO) {
		if(assetRepository.existsBySerialNumber(assetDTO.getSerialNumber())) {
			throw new ResourceNotFoundException("Asset already exists with serial number: " + assetDTO.getSerialNumber());
		}

		Category category = null;
		if(assetDTO.getCategoryId() != null) {
			category = categoryService.getCategoryById(assetDTO.getCategoryId());
		}

		Location location = null;
		if(assetDTO.getLocationId() != null) {
			location = locationService.getLocationById(assetDTO.getLocationId());
		}

		User assignedUser = null;
		if(assetDTO.getAssignedUserId() != null) {
			assignedUser = userService.getUserById(assetDTO.getAssignedUserId());
		}

		Asset asset = assetMapper.toEntity(assetDTO, category, location, assignedUser);
		Asset savedAsset = assetRepository.save(asset);
		AssetStatus previousStatus=asset.getStatus();
		asset.setAssignedUser(assignedUser);
		asset.setStatus(AssetStatus.IN_USE);
		asset.setPurchaseDate(LocalDateTime.now());
		assetRepository.save(asset);

		assert assignedUser != null;
		AssetHistory assetHistory=AssetHistory.builder()
				.asset(asset)
				.user(assignedUser)
				.previousStatus(previousStatus)
				.newStatus(asset.getStatus())
				.changeDate(LocalDateTime.now())
				.remarks("Assigned to user "+assignedUser.getFullName())
				.build();
		assetHistoryRepository.save(assetHistory);
		return assetMapper.toDTO(savedAsset);
	}

	public AssetDTO getAssetById(Long id) {
		Asset asset = assetRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
		return assetMapper.toDTO(asset);
	}

	public AssetDTO updateAsset(Long id, AssetDTO assetDTO) {
		Asset existingAsset = assetRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));

		Category category = null;
		if(assetDTO.getCategoryId() != null) {
			category = categoryService.getCategoryById(assetDTO.getCategoryId());
		}

		Location location = null;
		if(assetDTO.getLocationId() != null) {
			location = locationService.getLocationById(assetDTO.getLocationId());
		}

		User assignedUser = null;
		if(assetDTO.getAssignedUserId() != null) {
			assignedUser = userService.getUserById(assetDTO.getAssignedUserId());
		}

		existingAsset.setName(assetDTO.getName());
		existingAsset.setDescription(assetDTO.getDescription());
		existingAsset.setSerialNumber(assetDTO.getSerialNumber());
		existingAsset.setStatus(assetDTO.getStatus());
		existingAsset.setPurchaseDate(assetDTO.getPurchaseDate());
		existingAsset.setPrice(assetDTO.getPrice());
		existingAsset.setCategory(category);
		existingAsset.setLocation(location);
		existingAsset.setAssignedUser(assignedUser);

		Asset updatedAsset = assetRepository.save(existingAsset);
		return assetMapper.toDTO(updatedAsset);
	}

	public void deleteAsset(Long id) {
		Asset asset = assetRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
		assetRepository.delete(asset);
	}

	@Transactional
	public AssetDTO assignAssetToUser(Long assetId, Long userId) {
		Asset asset = assetRepository.findById(assetId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id " + assetId));
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

		if (asset.getAssignedUser() != null) {
			throw new IllegalStateException("Asset is already assigned to another user");
		}

		AssetStatus previousStatus = asset.getStatus();
		asset.setAssignedUser(user);
		asset.setStatus(AssetStatus.IN_USE);

		// Log trạng thái trước khi lưu
		System.out.println("Previous Status: " + previousStatus);
		System.out.println("New Status: " + asset.getStatus());

		assetRepository.save(asset);

		AssetHistory assetHistory = AssetHistory.builder()
				.asset(asset)
				.user(user)
				.previousStatus(previousStatus)
				.newStatus(asset.getStatus())
				.changeDate(LocalDateTime.now())
				.remarks("Assigned to user " + user.getFullName())
				.build();
		assetHistoryRepository.save(assetHistory);

		return assetMapper.toDTO(asset);
	}


	@Transactional
	public AssetDTO unassignAsset(Long assetId){
		Asset asset=assetRepository.findById(assetId)
				.orElseThrow(()->new ResourceNotFoundException("Asset not found with id "+assetId));

		if (asset.getAssignedUser() == null) {
			throw new IllegalStateException("Asset is not assigned to any user.");
		}

		User previousUser = asset.getAssignedUser();
		AssetStatus previousStatus = asset.getStatus();

		asset.setAssignedUser(null);
		asset.setStatus(AssetStatus.AVAILABLE);

		assetRepository.save(asset);
		// Ghi lại lịch sử thay đổi
		AssetHistory history = AssetHistory.builder()
				.asset(asset)
				.user(previousUser)
				.previousStatus(previousStatus)
				.newStatus(asset.getStatus())
				.changeDate(LocalDateTime.now())
				.remarks("Unassigned from user " + previousUser.getFullName())
				.build();

		assetHistoryRepository.save(history);

		return assetMapper.toDTO(asset);
	}


	public PageResponse<AssetDTOByUser> getAssetsByUserId(Long userId,int page,int size) {
		Pageable pageable=PageRequest.of(page,size);
		Page<Asset> assets = assetRepository.findByAssignedUserId(userId,pageable);
		List<AssetDTOByUser> assetDTOList = assets.getContent().stream()
				.map(assetMapper::toDTOByUser)
				.toList();
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

	public PageResponse<AssetHistoryDTO> getAssetHistory(Long assetId,int page,int size) {
		Pageable pageable=PageRequest.of(page,size);
		Page<AssetHistory> histories = assetHistoryRepository.findByAssetId(assetId,pageable);
		List<AssetHistoryDTO> assetHistoryDTOS=histories.getContent()
				.stream()
				.map(assetHistoryMapper::toDTO)
				.toList();
		return PageResponse.<AssetHistoryDTO>builder()
				.content(assetHistoryDTOS)
				.number(histories.getNumber())
				.size(histories.getSize())
				.totalElements(histories.getTotalElements())
				.totalPages(histories.getTotalPages())
				.first(histories.isFirst())
				.last(histories.isLast())
				.build();
	}
}
