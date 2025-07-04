package org.sang.labmanagement.semester;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sang.labmanagement.asset.category.Category;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/admin/semester")
@RequiredArgsConstructor
@Slf4j
public class SemesterController {
	private final SemesterService semesterService;


	@GetMapping
	@PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT') or hasRole('TEACHER')")
	public ResponseEntity<PageResponse<Semester>> getAllSemester(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
	) {
		return ResponseEntity.ok(semesterService.getAllSemester(page,size));
	}

	@PostMapping
	public ResponseEntity<Semester> createSemester(@RequestBody Semester semester) {
		return ResponseEntity.ok(semesterService.createSemester(semester));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Semester> updateSemester(@PathVariable Long id,
			@RequestBody Semester semester) {
		return ResponseEntity.ok(semesterService.updateSemester(id,semester));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteSemester(@PathVariable Long id) {
		semesterService.deleteSemester(id);
		return ResponseEntity.ok("Semester deleted successfully");
	}

	@GetMapping("/active")
	@PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT') or hasRole('TEACHER')")
	public ResponseEntity<List<Semester>> getActiveSemesters() {
		return ResponseEntity.ok(semesterService.getActiveSemesters());

	}

}
