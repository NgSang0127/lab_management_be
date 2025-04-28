package org.sang.labmanagement.timetable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.room.Room;
import org.sang.labmanagement.semester.Semester;
import org.sang.labmanagement.timetable.request.CreateTimetableRequest;
import org.sang.labmanagement.timetable.request.TimetableDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

public interface TimetableService {

	PageResponse<Timetable>getTimetables(Pageable pageable,String keyword,String roomName,String semesterIds);


	List<Timetable> getAllTimetableByWeek(LocalDate startDate,LocalDate endDate);

	List<Timetable> importExcelData(MultipartFile file) throws  Exception;

	Map<String,String> getFirstAndLastWeek(Long semesterId);

	boolean cancelTimetableOnDate(LocalDate cancelDate,int startLesson, String roomName,Long timetableId);

	List<Timetable> getTimetablesByDate(LocalDate date);

	Timetable createTimetable(CreateTimetableRequest request);

	Timetable getTimetableByCourse(String courseId,String NH,String TH,String studyTime);

	Timetable getTimetableByTimetableName(String timetableName);

	List<Semester> getFourSemesterRecent();

	Timetable createTimetableAdmin(TimetableDTO timetable);

	Timetable updateTimetable(Long id,TimetableDTO timetable);

	void deleteTimetable(Long id);

}
