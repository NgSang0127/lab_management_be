package org.sang.labmanagement.exception.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@JsonInclude(Include.NON_EMPTY)//json empty will ignore when convert json
public class ExceptionResponse {
	private Integer businessErrorCode;
	private String businessErrorDescription;
	private String error;
	private Set<String> validationErrors;
	private LocalDateTime timestamp;
	private Map<String,String> errors;


}
