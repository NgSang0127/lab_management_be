package org.sang.labmanagement.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetDTOByUser {

	private Long id;

	private String name;

	private String description;

	private String image;

	private AssetStatus status;

	private LocalDateTime purchaseDate;

	private Double price;

	private String categoryName;

	private String locationName;

	private String roomName;

	private Integer operationYear;

	private LocalDate operationStartDate;

	private OperationTime operationTime;

	private Integer lifeSpan;

}
