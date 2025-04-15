package org.sang.labmanagement.asset;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/assets")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

	private final AssetService assetService;

	@GetMapping
	public ResponseEntity<PageResponse<AssetDTO>> getAllAssets(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "roomName", required = false) String roomName,
			@RequestParam(name = "statuses", required = false) String statuses,
			@RequestParam(name = "categoryIds", required = false) String categoryIds,
			@RequestParam(name = "sortBy", required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = "asc", required = false) String sortOrder
	) {
		log.info("Fetching assets with params: page={}, size={}, keyword={}, roomName={}, statuses={}, categoryIds={}, sortBy={}, sortOrder={}",
				page, size, keyword, roomName, statuses, categoryIds, sortBy, sortOrder);
		PageResponse<AssetDTO> response = getCachedAssets(page, size, keyword, roomName, statuses, categoryIds, sortBy, sortOrder);
		log.info("Returning {} assets", response.getContent().size());
		return ResponseEntity.ok(response);
	}

	@Cacheable(value = "assets", key = "#page + '-' + #size + '-' + #keyword + '-' + #roomName + '-' + #statuses + '-' + #categoryIds + '-' + #sortBy + '-' + #sortOrder")
	public PageResponse<AssetDTO> getCachedAssets(
			int page, int size, String keyword, String roomName, String statuses,
			String categoryIds, String sortBy, String sortOrder
	) {
		Sort sort = Sort.unsorted();
		if (sortBy != null && !sortBy.isEmpty()) {
			Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
			sort = Sort.by(direction, sortBy);
		}
		Pageable pageable = PageRequest.of(page, size, sort);
		return assetService.getAllAssets(pageable, keyword, roomName, statuses, categoryIds);
	}

	@GetMapping("/{id}")
	@Cacheable(value = "asset", key = "#id")
	public ResponseEntity<AssetDTO> getAssetById(@PathVariable Long id) {
		AssetDTO assetDTO = assetService.getAssetById(id);
		return ResponseEntity.ok(assetDTO);
	}

	@PostMapping
	@CachePut(value = "asset", key = "#result.id")
	@CacheEvict(value = "assets", allEntries = true)
	public ResponseEntity<AssetDTO> createAsset(@RequestBody @Valid AssetDTO assetDTO) {
		AssetDTO createdAsset = assetService.createAsset(assetDTO);
		return ResponseEntity.ok(createdAsset);
	}

	@PostMapping("/{id}/duplicate")
	@CachePut(value = "asset", key = "#result.id")
	@CacheEvict(value = "assets", allEntries = true)
	public ResponseEntity<AssetDTO> duplicateAsset(@PathVariable Long id) {
		AssetDTO duplicated = assetService.duplicateAsset(id);
		return ResponseEntity.ok(duplicated);
	}

	@PutMapping("/{id}")
	@CachePut(value = "asset", key = "#id")
	@CacheEvict(value = "assets", allEntries = true)
	public ResponseEntity<AssetDTO> updateAsset(@PathVariable Long id, @RequestBody AssetDTO assetDTO) {
		AssetDTO updatedAsset = assetService.updateAsset(id, assetDTO);
		return ResponseEntity.ok(updatedAsset);
	}

	@DeleteMapping("/{id}")
	@CacheEvict(value = {"asset", "assets"}, key = "#id")
	public ResponseEntity<?> deleteAsset(@PathVariable Long id) {
		assetService.deleteAsset(id);
		return ResponseEntity.ok("Asset deleted successfully!");
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<PageResponse<AssetDTOByUser>> getAssetsByUserId(
			@PathVariable Long userId,
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "roomName", required = false) String roomName,
			@RequestParam(name = "statuses", required = false) String statuses,
			@RequestParam(name = "categoryIds", required = false) String categoryIds,
			@RequestParam(name = "sortBy", required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = "asc", required = false) String sortOrder
	) {
		Sort sort = Sort.unsorted();
		if (sortBy != null && !sortBy.isEmpty()) {
			Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
			sort = Sort.by(direction, sortBy);
		}
		Pageable pageable = PageRequest.of(page, size, sort);
		PageResponse<AssetDTOByUser> response = assetService.getAssetsByUserId(userId, pageable, keyword, roomName, statuses, categoryIds);
		return ResponseEntity.ok(response);
	}
}