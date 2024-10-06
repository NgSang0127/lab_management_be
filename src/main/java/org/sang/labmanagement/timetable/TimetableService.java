package org.sang.labmanagement.timetable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.sang.labmanagement.room.Room;
import org.sang.labmanagement.timetable.request.CreateTimetableRequest;
import org.springframework.web.multipart.MultipartFile;

public interface TimetableService {

	Timetable createTimetable(Timetable timetable);

	List<Timetable> getAllTimetableByWeek(LocalDate startDate,LocalDate endDate);

	List<Timetable> importExcelData(MultipartFile file) throws  Exception;

	Map<String,String> getFirstAndLastWeek();

	boolean cancelTimetableOnDate(LocalDate cancelDate,int startLesson, String roomName,Long timetableId);

	List<Timetable> getTimetablesByDate(LocalDate date);

	Timetable createTimetable(CreateTimetableRequest request);

	Timetable getTimetableByClassIdAndNhAndTh(String code,String NH,String TH,String timetableName);

}
