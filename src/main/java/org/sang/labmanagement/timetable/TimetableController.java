package org.sang.labmanagement.timetable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.timetable.request.CreateTimetableRequest;
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
			@RequestParam("startDate") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate startDate,
			@RequestParam("endDate") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate endDate
	) {
		return ResponseEntity.ok(timetableService.getAllTimetableByWeek(startDate, endDate));
	}


	@GetMapping("/weeks-range")
	public ResponseEntity<Map<String, String>> getFirstAndLastWeek() {
		Map<String, String> weekRange = timetableService.getFirstAndLastWeek();
		return ResponseEntity.ok(weekRange);
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

	@GetMapping("/course-details")
	public ResponseEntity<Timetable> getTimetableByClassIdAndNhAndTH(
			@RequestParam(required = false) String courseId,
			@RequestParam(required = false) String NH,
			@RequestParam(required = false) String TH,
			@RequestParam(required = false) String timetableName
	) {

		Timetable timetable = timetableService.getTimetableByClassIdAndNhAndTh(courseId, NH, TH, timetableName);

		if (timetable != null) {
			return ResponseEntity.ok(timetable);
		} else {
			return ResponseEntity.notFound().build();
		}
	}


	@PostMapping("/cancel")
	public ResponseEntity<String> cancelTimetable(
			@RequestParam String cancelDate,
			@RequestParam int startLesson,
			@RequestParam String roomName,
			@RequestParam Long timetableId) {

		LocalDate date = LocalDate.parse(cancelDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		boolean success = timetableService.cancelTimetableOnDate(date, startLesson, roomName, timetableId);

		if (success) {
			return ResponseEntity.ok("Timetable has been canceled successfully.");
		} else {
			return ResponseEntity.badRequest().body("Failed to cancel the timetable.");
		}
	}

	@GetMapping("/by-date")
	public ResponseEntity<List<Timetable>> getTimetablesByDate(
			@RequestParam("date") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate date) {

		List<Timetable> timetables = timetableService.getTimetablesByDate(date);
		return ResponseEntity.ok(timetables);
	}

	@PostMapping("/create")
	public ResponseEntity<Timetable> createTimetable(@RequestBody CreateTimetableRequest request) {
		Timetable newTimetable = timetableService.createTimetable(request);
		return ResponseEntity.ok(newTimetable);
	}

}
