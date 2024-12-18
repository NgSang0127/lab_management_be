package org.sang.labmanagement.logs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DailyLogStatistics {
	@JsonFormat(pattern = "dd/MM/yyyy")
	private LocalDate date;
	private long logCount;
}
