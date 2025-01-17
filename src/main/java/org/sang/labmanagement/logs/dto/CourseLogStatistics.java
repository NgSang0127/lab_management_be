package org.sang.labmanagement.logs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseLogStatistics {
	private Long courseId;
	private String courseName;
	private String NH;
	private String TH;
	private long logCount;
}
