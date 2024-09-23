package org.sang.labmanagement.timetable.LessonTime;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lesson-time")
@RequiredArgsConstructor
public class LessonTimeController {

	private final LessonTimeService lessonTimeService;

	@GetMapping
	private ResponseEntity<List<LessonTime>>getAllLessonTime(){
		return ResponseEntity.ok(lessonTimeService.getAllLessonTime());
	}

}
