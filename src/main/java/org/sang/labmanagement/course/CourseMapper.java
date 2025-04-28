package org.sang.labmanagement.course;

import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

	public CourseRequestDTO toDTO(Course course){
		CourseRequestDTO dto=new CourseRequestDTO();
		dto.setCode(course.getCode());
		dto.setDescription(course.getDescription());
		dto.setId(course.getId());
		dto.setNH(course.getNH());
		dto.setTH(course.getTH());
		dto.setCredits(course.getCredits());
		dto.setName(course.getName());
		dto.setInstructorId(course.getInstructor().getId());
		return dto;
	}

}
