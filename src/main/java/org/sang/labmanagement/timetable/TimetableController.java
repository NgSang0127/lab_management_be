package org.sang.labmanagement.timetable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.sang.labmanagement.timetable.request.CreateTimetableRequest;
import org.sang.labmanagement.timetable.request.TimetableDTO;
import org.sang.labmanagement.utils.TrackUserActivity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/timetable")
@RequiredArgsConstructor
public class TimetableController {

	private final TimetableService timetableService;


	@GetMapping
	public ResponseEntity<PageResponse<Timetable>>getTimetables(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "roomName", required = false) String roomName,
			@RequestParam(name = "semesterIds", required = false) String semesterIds,
			@RequestParam(name = "sortBy", required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = "asc", required = false) String sortOrder,
			@RequestParam(name = "status", defaultValue = "", required = false) String status
	){
		Sort sort = Sort.unsorted();
		if (sortBy != null && !sortBy.isEmpty()) {
			Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
			sort = Sort.by(direction, sortBy);
		}
		Pageable pageable = PageRequest.of(page, size, sort);
		return ResponseEntity.ok(timetableService.getTimetables(pageable, keyword, roomName, semesterIds,status));
	}

	@GetMapping("/by-week")
	public ResponseEntity<List<Timetable>> getTimetablesByWeek(
			@RequestParam("startDate") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate startDate,
			@RequestParam("endDate") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate endDate
	) {
		return ResponseEntity.ok(timetableService.getAllTimetableByWeek(startDate, endDate));
	}

	@GetMapping("/semester")
	public ResponseEntity<?> getFourSemesterRecent(){
		return ResponseEntity.ok(timetableService.getFourSemesterRecent());
	}

	@GetMapping("/weeks-range")
	public ResponseEntity<Map<String, String>> getFirstAndLastWeek(@RequestParam Long semesterId) {
		Map<String, String> weekRange = timetableService.getFirstAndLastWeek(semesterId);
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

	@TrackUserActivity
	@GetMapping("/course-details")
	public ResponseEntity<Timetable> getTimetableByClassIdAndNhAndTH(
			@RequestParam(required = false) String courseId,
			@RequestParam(required = false) String NH,
			@RequestParam(required = false) String TH,
			@RequestParam(required = false) String studyTime,
			@RequestParam(required = false) String timetableName
	) {
		Timetable timetable;

		if (courseId != null && NH != null  && studyTime !=null) {
			// Tìm theo Course nếu có courseId, NH, và TH
			timetable = timetableService.getTimetableByCourse(courseId, NH, TH,studyTime);
		} else if (timetableName != null) {
			// Tìm theo timetableName nếu không có Course
			timetable = timetableService.getTimetableByTimetableName(timetableName);
		} else {
			return ResponseEntity.badRequest().build(); // Trả về lỗi nếu không có thông tin tìm kiếm
		}

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

	@GetMapping("/by-date-room")
	public ResponseEntity<List<Timetable>> getTimetablesByDate(
			@RequestParam LocalDate date,
			@RequestParam(required = false) String roomName
	) {
		List<Timetable> timetables = timetableService.getTimetablesByDateAndRoom(date, roomName);
		return ResponseEntity.ok(timetables);
	}


	@PostMapping("/create")
	public ResponseEntity<Timetable> createTimetable(@RequestBody CreateTimetableRequest request) {
		Timetable newTimetable = timetableService.createTimetable(request);
		return ResponseEntity.ok(newTimetable);
	}


	@PostMapping("/createAdmin")
	public ResponseEntity<Timetable> createTimetable(@RequestBody TimetableDTO request) {
		Timetable newTimetable = timetableService.createTimetableAdmin(request);
		return ResponseEntity.ok(newTimetable);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Timetable>updateTimetable(@PathVariable Long id,@RequestBody TimetableDTO request){
		return ResponseEntity.ok(timetableService.updateTimetable(id,request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteTimetable(@PathVariable Long id) {
		try {
			timetableService.deleteTimetable(id);
			return ResponseEntity.ok("Timetable has been deleted successfully.");
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.badRequest().body("Error deleting timetable: " + e.getMessage());
		}
	}

	@PatchMapping("/{id}/approve")
	public ResponseEntity<Timetable> approveTimetable(@PathVariable Long id) {
		Timetable timetable = timetableService.approveTimetable(id);
		return ResponseEntity.ok(timetable);
	}

	@PatchMapping("/{id}/reject")
	public ResponseEntity<Timetable> rejectTimetable(@PathVariable Long id) {
		Timetable timetable = timetableService.rejectTimetable(id);
		return ResponseEntity.ok(timetable);
	}

}
