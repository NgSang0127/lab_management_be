package org.sang.labmanagement.asset.location;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/locations")
@RequiredArgsConstructor
@Slf4j
public class LocationController {
	private final LocationService locationService;

	@GetMapping
	public ResponseEntity<PageResponse<Location>> getAllLocations(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
	) {
		log.info("Fetching locations with params: page={}, size={}", page, size);
		PageResponse<Location> response = getCachedLocations(page, size);
		log.info("Returning {} locations", response.getContent().size());
		return ResponseEntity.ok(response);
	}

	@Cacheable(value = "locations", key = "#page + '-' + #size")
	public PageResponse<Location> getCachedLocations(int page, int size) {
		return locationService.getAllLocations(page, size);
	}

	@GetMapping("/{id}")
	@Cacheable(value = "location", key = "#id")
	public ResponseEntity<Location> getLocationById(@PathVariable Long id) {
		log.info("Fetching location with id: {}", id);
		Location location = locationService.getLocationById(id);
		log.info("Returning location: {}", location.getId());
		return ResponseEntity.ok(location);
	}

	@PostMapping
	@CachePut(value = "location", key = "#result.id")
	@CacheEvict(value = "locations", allEntries = true)
	public ResponseEntity<Location> createLocation(@RequestBody Location location) {
		log.info("Creating location: {}", location);
		Location createdLocation = locationService.createLocation(location);
		log.info("Created location with id: {}", createdLocation.getId());
		return ResponseEntity.ok(createdLocation);
	}

	@PutMapping("/{id}")
	@CachePut(value = "location", key = "#id")
	@CacheEvict(value = "locations", allEntries = true)
	public ResponseEntity<Location> updateLocation(@PathVariable Long id, @RequestBody Location locationDetails) {
		log.info("Updating location with id: {}", id);
		Location updatedLocation = locationService.updateLocation(id, locationDetails);
		log.info("Updated location: {}", updatedLocation.getId());
		return ResponseEntity.ok(updatedLocation);
	}

	@DeleteMapping("/{id}")
	@CacheEvict(value = {"location", "locations"}, key = "#id")
	public ResponseEntity<?> deleteLocation(@PathVariable Long id) {
		log.info("Deleting location with id: {}", id);
		locationService.deleteLocation(id);
		log.info("Deleted location with id: {}", id);
		return ResponseEntity.ok("Location deleted successfully!");
	}
}