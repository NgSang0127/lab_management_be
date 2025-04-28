package org.sang.labmanagement.course;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {
	private final CourseService courseService;

	@GetMapping
	public ResponseEntity<PageResponse<CourseRequestDTO>>getCourses(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size,
			@RequestParam(name = "keyword", required = false) String keyword
	){
		return ResponseEntity.ok(courseService.getCourses(page,size,keyword));
	}
}
