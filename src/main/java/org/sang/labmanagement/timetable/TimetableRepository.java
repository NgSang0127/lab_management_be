package org.sang.labmanagement.timetable;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable,Long>, JpaSpecificationExecutor<Timetable> {

	@Query("SELECT t FROM Timetable t JOIN t.courses c " +
			"WHERE c.code = :courseId AND c.NH = :nh AND c.TH = :th AND t.studyTime = :studyTime")
	Timetable findByCourseAndStudyTime(@Param("courseId") String courseId,
			@Param("nh") String nh,
			@Param("th") String th,
			@Param("studyTime") String studyTime);


	Timetable findByTimetableName(String timetableName);

	Optional<Timetable> findByTimetableNameAndSemesterId(String timetableName, Long semesterId);

	@Query("""
            SELECT t FROM Timetable t
            JOIN t.room r
            JOIN t.instructor i
            JOIN t.courses c
            WHERE t.classId = :classId
            AND r.name = :roomName
            AND t.studyTime =:studyTime
            AND c.NH =:NH
            AND c.TH =:TH
            """)
	Optional<Timetable> findByClassIdAndRoomNameAndStudyTimeAndTHAndNH(
			String classId,
			String roomName,
			String studyTime,
			String TH,
			String NH

	);


	List<Timetable> findBySemesterId(Long semesterId);

	@Query("SELECT t FROM Timetable t WHERE t.semester.id = :semesterId " +
			"AND t.dayOfWeek = :dayOfWeek AND t.room.id = :roomId " +
			"AND ((t.startLessonTime.lessonNumber <= :endLessonNumber AND t.endLessonTime.lessonNumber >= :startLessonNumber) OR " +
			"(t.startLessonTime.lessonNumber >= :startLessonNumber AND t.endLessonTime.lessonNumber <= :endLessonNumber))")
	List<Timetable> findConflictingTimetables(Long semesterId, DayOfWeek dayOfWeek, Long roomId,
			int startLessonNumber, int endLessonNumber);


	@Query("SELECT t FROM Timetable t WHERE t.room IS NOT NULL")
	List<Timetable> findAllWithRoom();
}




