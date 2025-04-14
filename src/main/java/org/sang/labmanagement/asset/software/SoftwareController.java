package org.sang.labmanagement.asset.software;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/admin/softwares")
@RequiredArgsConstructor
public class SoftwareController {
	private final SoftwareService softwareService;

	@GetMapping
	public ResponseEntity<PageResponse<Software>>getAllSoftwares(
			@RequestParam(name="page",defaultValue = "0",required = false) int page,
			@RequestParam(name ="size",defaultValue = "10",required = false) int size
			){
		return ResponseEntity.ok(softwareService.getAllSoftwares(page,size));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Software> getSoftwareById(@PathVariable Long id){
		Software software=softwareService.getSoftwareById(id);
		return ResponseEntity.ok(software);
	}

	@PostMapping
	public ResponseEntity<Software> createSoftware(@RequestBody Software software){
		Software createdSoftware=softwareService.createSoftware(software);
		return ResponseEntity.ok(createdSoftware);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Software> updateSoftware(@PathVariable Long id,@RequestBody Software softwareDetails){
		Software updatedSoftware=softwareService.updateSoftware(id,softwareDetails);
		return ResponseEntity.ok(updatedSoftware);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteSoftware(@PathVariable Long id){
		softwareService.deleteSoftware(id);
		return ResponseEntity.ok("Software deleted successfully!");
	}

	@GetMapping("/by-room/{roomId}")
	public ResponseEntity<List<Software>> getSoftwaresByRoomId(@PathVariable Long roomId) {
		return ResponseEntity.ok(softwareService.getSoftwaresByRoomId(roomId));
	}
}
