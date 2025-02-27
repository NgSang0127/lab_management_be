package org.sang.labmanagement.timetable;


import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable,Long> {

	@Query("SELECT t FROM Timetable t JOIN t.courses c " +
			"WHERE c.code = :courseId AND c.NH = :nh AND c.TH = :th AND t.studyTime = :studyTime")
	Timetable findByCourseAndStudyTime(@Param("courseId") String courseId,
			@Param("nh") String nh,
			@Param("th") String th,
			@Param("studyTime") String studyTime);


	Timetable findByTimetableName(String timetableName);


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








}
