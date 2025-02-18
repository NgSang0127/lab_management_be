package org.sang.labmanagement.asset;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.asset.asset_history.AssetHistoryDTO;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/assets")
@RequiredArgsConstructor
public class AssetController {

	private final AssetService assetService;


	@GetMapping
	public ResponseEntity<PageResponse<AssetDTO>> getAllAssets(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "status", required = false) String status
	) {
		return ResponseEntity.ok(assetService.getAllAssets(page, size,keyword,status));
	}


	@GetMapping("/{id}")
	public ResponseEntity<AssetDTO> getAssetById(@PathVariable Long id) {
		AssetDTO assetDTO = assetService.getAssetById(id);
		return ResponseEntity.ok(assetDTO);
	}


	@PostMapping
	public ResponseEntity<AssetDTO> createAsset(@RequestBody @Valid AssetDTO assetDTO) {
		AssetDTO createdAsset = assetService.createAsset(assetDTO);
		return ResponseEntity.ok(createdAsset);
	}


	@PutMapping("/{id}")
	public ResponseEntity<AssetDTO> updateAsset(@PathVariable Long id, @RequestBody AssetDTO assetDTO) {
		AssetDTO updatedAsset = assetService.updateAsset(id, assetDTO);
		return ResponseEntity.ok(updatedAsset);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteAsset(@PathVariable Long id) {
		assetService.deleteAsset(id);
		return ResponseEntity.ok("Asset deleted successfully!");
	}

	// Endpoint để gán tài sản cho người dùng

	@PostMapping("/{assetId}/assign/{userId}")
	public ResponseEntity<AssetDTO> assignAssetToUser(@PathVariable Long assetId, @PathVariable Long userId) {
		AssetDTO assignedAsset = assetService.assignAssetToUser(assetId, userId);
		return ResponseEntity.ok(assignedAsset);
	}

	// Endpoint để bỏ gán tài sản

	@PostMapping("/{assetId}/unassign")
	public ResponseEntity<AssetDTO> unassignAsset(@PathVariable Long assetId) {
		AssetDTO unassignedAsset = assetService.unassignAsset(assetId);
		return ResponseEntity.ok(unassignedAsset);
	}

	// Endpoint để lấy tài sản theo người dùng
	@GetMapping("/user/{userId}")
	public ResponseEntity<PageResponse<AssetDTOByUser>> getAssetsByUserId(
			@PathVariable Long userId,
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
	) {

		return ResponseEntity.ok(assetService.getAssetsByUserId(userId, page, size));
	}


	@GetMapping("/{assetId}/history")
	public ResponseEntity<PageResponse<AssetHistoryDTO>> getAssetHistory(
			@PathVariable Long assetId,
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
	) {

		return ResponseEntity.ok(assetService.getAssetHistory(assetId, page, size));
	}

}
