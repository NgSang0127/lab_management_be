package org.sang.labmanagement.asset.history;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.asset.Asset;
import org.sang.labmanagement.asset.AssetDTO;
import org.sang.labmanagement.asset.AssetDTOByUser;
import org.sang.labmanagement.asset.AssetMapper;
import org.sang.labmanagement.asset.AssetRepository;
import org.sang.labmanagement.asset.AssetStatus;
import org.sang.labmanagement.asset.OperationTime;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetHistoryService {

	private final AssetHistoryRepository assetHistoryRepository;
	private final AssetRepository assetRepository;
	private final AssetHistoryMapper assetHistoryMapper;
	private final AssetMapper assetMapper;
	private final UserRepository userRepository;

	@Transactional
	public AssetDTO assignAssetToUser(Long assetId, Long userId) {
		Asset asset = assetRepository.findById(assetId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		if (asset.getAssignedUser() != null) {
			throw new IllegalStateException("Asset is already assigned to another user");
		}

		AssetStatus previousStatus = asset.getStatus();
		asset.setAssignedUser(user);
		asset.setStatus(AssetStatus.IN_USE);

		if (asset.getOperationStartDate() == null) {
			asset.setOperationStartDate(LocalDate.now());
		}
		if (asset.getOperationYear() == null) {
			asset.setOperationYear(LocalDateTime.now().getYear());
		}
		if (asset.getOperationTime() == null) {
			asset.setOperationTime(OperationTime.FULL_DAY);
		}

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
	public AssetDTO unassignAsset(Long assetId) {
		Asset asset = assetRepository.findById(assetId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

		if (asset.getAssignedUser() == null) {
			throw new IllegalStateException("Asset is not assigned to any user.");
		}

		User previousUser = asset.getAssignedUser();
		AssetStatus previousStatus = asset.getStatus();

		asset.setAssignedUser(null);
		asset.setStatus(AssetStatus.AVAILABLE);
		asset.setOperationTime(OperationTime.FULL_DAY);

		assetRepository.save(asset);

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


	public PageResponse<AssetHistoryDTO> getAssetHistory(Pageable pageable,String keyword, String statuses) {
		Specification<AssetHistory>spec=AssetHistorySpecification.filterByKeywordAndStatuses(keyword,statuses);

		Page<AssetHistory> histories = assetHistoryRepository.findAll(spec,pageable);
		List<AssetHistoryDTO> assetHistoryDTOS = histories.getContent()
				.stream()
				.map(assetHistoryMapper::toDTO)
				.collect(Collectors.toList());
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
