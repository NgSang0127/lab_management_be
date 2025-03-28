package org.sang.labmanagement.asset.location;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.ResourceAlreadyExistsException;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {
	private final LocationRepository locationRepository;

	@Cacheable(value = "locations", key = "#page + '-' + #size")
	public PageResponse<Location> getAllLocations(int page,int size) {
		Pageable pageable= PageRequest.of(page,size);
		Page<Location>locations=locationRepository.findAll(pageable);
		return PageResponse.<Location>builder()
				.content(locations.getContent())
				.number(locations.getNumber())
				.size(locations.getSize())
				.totalElements(locations.getTotalElements())
				.totalPages(locations.getTotalPages())
				.first(locations.isFirst())
				.last(locations.isLast())
				.build();
	}

	@CacheEvict(value = "locations", allEntries = true)
	public Location createLocation(Location location) {
		if(locationRepository.existsByName(location.getName())) {
			throw new ResourceAlreadyExistsException("Location already exists with name: " + location.getName());
		}
		return locationRepository.save(location);
	}

	@Cacheable(value = "location" ,key="#id")
	public Location getLocationById(Long id) {
		return locationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + id));
	}

	@CacheEvict(value = {"location", "locations"}, key = "#id", allEntries = true)
	public Location updateLocation(Long id, Location locationDetails) {
		Location location = getLocationById(id);
		location.setName(locationDetails.getName());
		location.setAddress(locationDetails.getAddress());
		return locationRepository.save(location);
	}

	@CacheEvict(value = {"location", "locations"}, key = "#id", allEntries = true)
	public void deleteLocation(Long id) {
		Location location = getLocationById(id);
		locationRepository.delete(location);
	}
}
