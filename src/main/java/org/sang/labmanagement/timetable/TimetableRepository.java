package org.sang.labmanagement.timetable;


import java.time.DayOfWeek;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable,Long> {

	@Query("SELECT t FROM Timetable t LEFT JOIN t.courses c " +
			"WHERE (c.code = :code AND c.NH = :nh AND c.TH = :th) " +
			"OR (c IS NULL AND t.timetableName = :timetableName)")
	Timetable findByCourseOrTimetableName(@Param("code") String code,
			@Param("nh") String nh,
			@Param("th") String th,
			@Param("timetableName") String timetableName);


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
}
