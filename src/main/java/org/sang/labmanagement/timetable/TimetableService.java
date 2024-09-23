package org.sang.labmanagement.timetable;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface TimetableService {

	Timetable createTimetable(Timetable timetable);

	List<Timetable> getAllTimetableByWeek(LocalDate startDate,LocalDate endDate);

	List<Timetable> importExcelData(MultipartFile file) throws  Exception;



}
