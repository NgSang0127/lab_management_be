package org.sang.labmanagement.timetable;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.course.CourseMapper;
import org.sang.labmanagement.course.CourseRequestDTO;
import org.sang.labmanagement.timetable.request.TimetableDTO;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimetableMapper {
	private final CourseMapper courseMapper;

	public TimetableDTO toDto(Timetable timetable){
		TimetableDTO dto=new TimetableDTO();
		dto.setId(timetable.getId());
		dto.setTimetableName(timetable.getTimetableName());
		dto.setSemesterId(timetable.getSemester().getId());
		dto.setInstructorId(timetable.getInstructor().getId());
		dto.setDayOfWeek(timetable.getDayOfWeek());
		dto.setRoomId(timetable.getRoom().getId());
		dto.setNumberOfStudents(timetable.getNumberOfStudents());
		dto.setTotalLessonSemester(timetable.getTotalLessonSemester());
		dto.setTotalLessonDay(timetable.getTotalLessonDay());
		dto.setClassId(timetable.getClassId());
		dto.setStudyTime(timetable.getStudyTime());
		dto.setStartLessonTimeId(timetable.getStartLessonTime().getId());
		dto.setEndLessonTimeId(timetable.getEndLessonTime().getId());
		dto.setDescription(timetable.getDescription());
		dto.setCancelDates(timetable.getCancelDates());

		List<CourseRequestDTO> dtos=timetable.getCourses()
				.stream()
				.map(courseMapper::toDTO)
				.toList();
		dto.setCourses(dtos);

		return dto;

	}
}
