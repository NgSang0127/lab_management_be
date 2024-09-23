package org.sang.labmanagement.timetable.LessonTime;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonTimeServiceImplement implements LessonTimeService{

	private final LessonTimeRepository lessonTimeRepository;

	@Override
	public List<LessonTime> getAllLessonTime() {
		return lessonTimeRepository.findAll();
	}
}
