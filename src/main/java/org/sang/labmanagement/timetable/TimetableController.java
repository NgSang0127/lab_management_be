package org.sang.labmanagement.timetable;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/timetable")
@RequiredArgsConstructor
public class TimetableController {

	private final TimetableService timetableService;

	@PostMapping
	private ResponseEntity<Timetable> createTimetable(@RequestBody Timetable timetable){
		return ResponseEntity.ok(timetableService.createTimetable(timetable));
	}

	@GetMapping("/by-week")
	public ResponseEntity<List<Timetable>> getTimetablesByWeek(
			@RequestParam("startDate") @DateTimeFormat(iso = ISO.DATE) String startDateString,
			@RequestParam("endDate") @DateTimeFormat(iso = ISO.DATE) String endDateString
	){
		LocalDate startDate = LocalDate.parse(startDateString.trim());
		LocalDate endDate = LocalDate.parse(endDateString.trim());

		return ResponseEntity.ok(timetableService.getAllTimetableByWeek(startDate, endDate));
	}


	@PostMapping("/import")
	public ResponseEntity<?> importTimetableData(@RequestParam("file") MultipartFile file) {
		try {
			List<Timetable> importedTimetables = timetableService.importExcelData(file);
			return ResponseEntity.ok(importedTimetables);
		} catch (Exception e) {
			// Gửi lại lỗi chi tiết trong phản hồi
			return ResponseEntity.badRequest().body("Error importing timetables: " + e.getMessage());
		}
	}


}
