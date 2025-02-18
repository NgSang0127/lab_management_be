package org.sang.labmanagement.asset.maintenance;

import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/admin/maintenances")
@RequiredArgsConstructor
public class MaintenanceController {
	private final MaintenanceService maintenanceService;


	@GetMapping
	public ResponseEntity<PageResponse<MaintenanceDTO>> getAllMaintenances(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size,
			@RequestParam(name= "keyword", required = false) String keyword,
			@RequestParam(name= "status", required = false) String status
	){
		return ResponseEntity.ok(maintenanceService.getAllMaintenances(page, size,keyword,status));
	}

	@GetMapping("/{id}")
	public ResponseEntity<MaintenanceDTO> getMaintenanceById(@PathVariable Long id) {
		MaintenanceDTO maintenanceDTO = maintenanceService.getMaintenanceById(id);
		return ResponseEntity.ok(maintenanceDTO);
	}

	@PostMapping
	public ResponseEntity<MaintenanceDTO> createMaintenance(@RequestBody MaintenanceDTO maintenanceDTO) {
		MaintenanceDTO createdMaintenance = maintenanceService.createMaintenance(maintenanceDTO);
		return ResponseEntity.ok(createdMaintenance);
	}


	@PutMapping("/{id}")
	public ResponseEntity<MaintenanceDTO> updateMaintenance(@PathVariable Long id, @RequestBody MaintenanceDTO maintenanceDTO) {
		MaintenanceDTO updatedMaintenance = maintenanceService.updateMaintenance(id, maintenanceDTO);
		return ResponseEntity.ok(updatedMaintenance);
	}


	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteMaintenance(@PathVariable Long id) {
		maintenanceService.deleteMaintenance(id);
		return ResponseEntity.ok("Maintenance deleted successfully!");
	}
}
