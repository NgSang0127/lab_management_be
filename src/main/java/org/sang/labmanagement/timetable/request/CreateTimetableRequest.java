package org.sang.labmanagement.timetable.request;


import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateTimetableRequest {
	private String timetableName;

	private String roomName;

	private int startLesson;

	private int endLesson;

	private LocalDate date;

	private String instructorId;

	private String description;
}
