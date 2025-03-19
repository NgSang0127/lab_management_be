package org.sang.labmanagement.asset.maintenance;

import com.sun.tools.javac.Main;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.asset.Asset;
import org.sang.labmanagement.asset.AssetRepository;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MaintenanceService {
	private final MaintenanceRepository maintenanceRepository;
	private final AssetRepository assetRepository;
	private final MaintenanceMapper maintenanceMapper;

	@Cacheable(value = "maintenances",key="#page + '-' + #size")
	public PageResponse<MaintenanceDTO> getAllMaintenances(int page,int size,String keyword,String status){
		Pageable pageable= PageRequest.of(page,size);
		Specification<Maintenance>spec=MaintenanceSpecification.getAssetsByKeywordAndStatus(keyword, status);
		Page<Maintenance>maintenancePage=maintenanceRepository.findAll(spec,pageable);

		List<MaintenanceDTO> maintenanceDTOS=maintenancePage.getContent()
				.stream()
				.map(maintenanceMapper::toDTO)
				.toList();
		return PageResponse.<MaintenanceDTO>builder()
				.content(maintenanceDTOS)
				.number(maintenancePage.getNumber())
				.size(maintenancePage.getSize())
				.totalElements(maintenancePage.getTotalElements())
				.totalPages(maintenancePage.getTotalPages())
				.last(maintenancePage.isLast())
				.build();
	};

	@Cacheable(value = "maintenance",key="#id")
	public MaintenanceDTO getMaintenanceById(Long id) {
		Maintenance maintenance = maintenanceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Maintenance not found with id " + id));
		return maintenanceMapper.toDTO(maintenance);
	}

	@CacheEvict(value = "maintenances",allEntries = true)
	public MaintenanceDTO createMaintenance(MaintenanceDTO maintenanceDTO){
		Asset asset=assetRepository.findById(maintenanceDTO.getAssetId())
				.orElseThrow(()->new ResourceNotFoundException("Asset not found with id "+maintenanceDTO.getAssetId()));
		Maintenance maintenance=maintenanceMapper.toEntity(maintenanceDTO,asset);
		maintenance.setStatus(MaintenanceStatus.SCHEDULED);
		Maintenance savedMaintenance=maintenanceRepository.save(maintenance);
		return maintenanceMapper.toDTO(savedMaintenance);
	}

	@CacheEvict(value = {"maintenance","maintenances"} ,allEntries = true)
	public MaintenanceDTO updateMaintenance(Long id, MaintenanceDTO maintenanceDTO) {
		Maintenance existingMaintenance = maintenanceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Maintenance not found with id " + id));

		if (maintenanceDTO.getAssetId() != null) {
			Asset asset = assetRepository.findById(maintenanceDTO.getAssetId())
					.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id " + maintenanceDTO.getAssetId()));
			existingMaintenance.setAsset(asset);
		}

		existingMaintenance.setScheduleDate(maintenanceDTO.getScheduledDate());
		existingMaintenance.setStatus(maintenanceDTO.getStatus());
		existingMaintenance.setRemarks(maintenanceDTO.getRemarks());

		Maintenance updatedMaintenance = maintenanceRepository.save(existingMaintenance);
		return maintenanceMapper.toDTO(updatedMaintenance);
	}

	@CacheEvict(value = {"maintenance","maintenances"},allEntries = true)
	public void deleteMaintenance(Long id) {
		Maintenance maintenance = maintenanceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Maintenance not found with id " + id));
		maintenanceRepository.delete(maintenance);
	}

	public List<MaintenanceDTO> getMaintenancesByScheduledDate(LocalDateTime start, LocalDateTime end) {
		List<Maintenance> maintenances = maintenanceRepository.findByScheduleDateBetween(start, end);
		return maintenances.stream()
				.map(maintenanceMapper::toDTO)
				.collect(Collectors.toList());
	}



	public String getEmailByAssetId(Long assetId) {
		Asset asset = assetRepository.findById(assetId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id " + assetId));
		if (asset.getAssignedUser() != null) {
			return asset.getAssignedUser().getEmail();
		}
		return "admin@example.com"; // Địa chỉ email mặc định nếu tài sản chưa được gán
	}

	public String getUserNameByAssetId(Long assetId) {
		Asset asset = assetRepository.findById(assetId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id " + assetId));
		if (asset.getAssignedUser() != null) {
			return asset.getAssignedUser().getUsername();
		}
		return "Admin"; // Địa chỉ email mặc định nếu tài sản chưa được gán
	}

	public String getAssetNameById(Long assetId) {
		Asset asset = assetRepository.findById(assetId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found with id " + assetId));
		return asset.getName();
	}



}
