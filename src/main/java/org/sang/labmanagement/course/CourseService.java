package org.sang.labmanagement.course;

import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.user.instructor.Instructor;
import org.sang.labmanagement.user.instructor.InstructorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseService {
	private final CourseRepository courseRepository;
	private final InstructorRepository instructorRepository;
	private final CourseMapper courseMapper;

	@Transactional
	public Course createCourse(CourseRequestDTO request) {

		Optional<Course> existingCourse = courseRepository.findByCodeAndNHAndTH(
				request.getCode(), request.getNH(), request.getTH());
		if (existingCourse.isPresent()) {
			throw new IllegalArgumentException("Course with code " + request.getCode() +
					", NH " + request.getNH() + ", TH " + request.getTH() + " already exists");
		}

		Instructor instructor = instructorRepository.findById(request.getInstructorId())
				.orElseThrow(() -> new IllegalArgumentException("Instructor not found with ID: " + request.getInstructorId()));

		Course course = Course.builder()
				.name(request.getName())
				.code(request.getCode())
				.NH(request.getNH())
				.TH(request.getTH())
				.description(request.getDescription())
				.credits(request.getCredits())
				.instructor(instructor)
				.enrollments(new HashSet<>())
				.timetables(new HashSet<>())
				.build();

		return courseRepository.save(course);
	}


	public PageResponse<CourseRequestDTO>getCourses(int page,int size,String keyword){
		Pageable pageable= PageRequest.of(page,size);
		Specification<Course> spec=CourseSpecification.getCoursesByKeyword(keyword);
		Page<Course> courses=courseRepository.findAll(spec,pageable);
		List<CourseRequestDTO> courseRequestDTOS=courses.getContent()
				.stream()
				.map(courseMapper::toDTO)
				.toList();
		return PageResponse.<CourseRequestDTO>builder()
				.content(courseRequestDTOS)
				.number(courses.getNumber())
				.size(courses.getSize())
				.totalElements(courses.getTotalElements())
				.totalPages(courses.getTotalPages())
				.first(courses.isFirst())
				.last(courses.isLast())
				.build();
	}
}
