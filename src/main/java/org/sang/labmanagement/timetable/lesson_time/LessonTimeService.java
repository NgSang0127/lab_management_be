package org.sang.labmanagement.timetable.lesson_time;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonTimeService {
	private final LessonTimeRepository lessonTimeRepository;

	public List<LessonTime> getAllLessonTime() {
		return lessonTimeRepository.findAll();
	}
}
