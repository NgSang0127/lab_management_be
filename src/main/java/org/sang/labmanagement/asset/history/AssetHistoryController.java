package org.sang.labmanagement.asset.history;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.asset.AssetDTO;
import org.sang.labmanagement.asset.AssetDTOByUser;
import org.sang.labmanagement.asset.borrow.AssetBorrowingDTO;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class AssetHistoryController {

	private final AssetHistoryService assetHistoryService;

	@PostMapping("/{assetId}/assign/{userId}")
	@PreAuthorize("hasRole('OWNER') or hasRole('CO_OWNER') or hasRole('ADMIN')")
	public ResponseEntity<AssetDTO> assignAssetToUser(@PathVariable Long assetId, @PathVariable Long userId) {
		AssetDTO assignedAsset = assetHistoryService.assignAssetToUser(assetId, userId);
		return ResponseEntity.ok(assignedAsset);
	}

	// Endpoint để bỏ gán tài sản

	@PostMapping("/{assetId}/unassign")
	@PreAuthorize("hasRole('OWNER') or hasRole('CO_OWNER') or hasRole('ADMIN')")
	public ResponseEntity<AssetDTO> unassignAsset(@PathVariable Long assetId) {
		AssetDTO unassignedAsset = assetHistoryService.unassignAsset(assetId);
		return ResponseEntity.ok(unassignedAsset);
	}


	@GetMapping()
	public ResponseEntity<PageResponse<AssetHistoryDTO>> getAssetHistory(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "statuses", required = false) String statuses,
			@RequestParam(name = "sortBy", required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = "asc", required = false) String sortOrder

			) {

		Sort sort = Sort.unsorted();
		if (sortBy != null && !sortBy.isBlank()) {
			String mappedSortBy;
			Sort.Direction direction = sortOrder.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
			switch (sortBy) {
				case "assetName":
					mappedSortBy = "asset.name";
					break;
				case "username":
					mappedSortBy = "user.username";
					break;
				case "id":
				case "changeDate":
				case "newStatus":
				case "previousStatus":
				case "remarks":
					mappedSortBy = sortBy;
					break;
				default:
					mappedSortBy = "id";
			}
			sort = Sort.by(direction, mappedSortBy);
		}

		Pageable pageable = PageRequest.of(page, size, sort);
		PageResponse<AssetHistoryDTO> response = assetHistoryService.getAssetHistory(pageable, keyword, statuses);
		return ResponseEntity.ok(response);
	}
}
