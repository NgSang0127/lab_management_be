package org.sang.labmanagement.timetable.lesson_time;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonTimeRepository extends JpaRepository<LessonTime,Long> {
	List<LessonTime>findAll();

	LessonTime findByLessonNumber(int startLesson);

}
