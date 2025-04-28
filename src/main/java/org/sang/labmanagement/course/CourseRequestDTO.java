package org.sang.labmanagement.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseRequestDTO {
	private Long id; // ID của Course có sẵn (TH2)
	private String name; // TH3
	private String code; // TH3
	private String NH; // TH3
	private String TH; // TH3
	private String description; // TH3
	private Integer credits; // TH3
	private Long instructorId; // TH3
}
