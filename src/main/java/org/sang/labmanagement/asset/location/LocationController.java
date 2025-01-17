package org.sang.labmanagement.asset.location;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.asset.category.Category;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("api/v1/admin/locations")
@RequiredArgsConstructor
public class LocationController {
	private final LocationService locationService;

	@GetMapping
	public ResponseEntity<PageResponse<Location>>getAllLocations(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
	){
		return ResponseEntity.ok(locationService.getAllLocations(page,size));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Location> getLocationById(@PathVariable Long id) {
		Location location = locationService.getLocationById(id);
		return ResponseEntity.ok(location);
	}


	@PostMapping
	public ResponseEntity<Location> createLocation(@RequestBody Location location) {
		Location createdLocation = locationService.createLocation(location);
		return ResponseEntity.ok(createdLocation);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Location> updateLocation(@PathVariable Long id, @RequestBody Location locationDetails) {
		Location updatedLocation = locationService.updateLocation(id, locationDetails);
		return ResponseEntity.ok(updatedLocation);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteLocation(@PathVariable Long id) {
		locationService.deleteLocation(id);
		return ResponseEntity.ok("Location deleted successfully!");
	}
}
