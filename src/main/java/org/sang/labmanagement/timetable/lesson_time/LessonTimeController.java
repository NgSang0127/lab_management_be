package org.sang.labmanagement.timetable.lesson_time;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lesson-time")
@RequiredArgsConstructor
public class LessonTimeController {

	private final LessonTimeService lessonTimeService;


	@GetMapping
	public ResponseEntity<List<LessonTime>>getAllLessonTime(){
		return ResponseEntity.ok(lessonTimeService.getAllLessonTime());
	}

}
