package org.sang.labmanagement.timetable;


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



}
