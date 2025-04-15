package org.sang.labmanagement.asset;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Data;
import org.sang.labmanagement.asset.configuration.AssetConfigDTO;

@Data
public class AssetDTO {

	private Long id;

	@NotBlank(message = "Asset name is required")
	private String name;

	private String description;

	private String image;


	@NotNull(message = "Asset status is required")
	private AssetStatus status;

	@PastOrPresent(message = "Purchase date cannot be in the future")
	private LocalDateTime purchaseDate;

	@PositiveOrZero(message = "Price must be zero or positive")
	private Double price;

	@NotNull(message = "Category ID is required")
	private Long categoryId;

	@NotNull(message = "Location ID is required")
	private Long locationId;

	private Long roomId;

	private Integer quantity;

	private Integer warranty;

	private Integer operationYear;

	private LocalDate operationStartDate;

	private OperationTime operationTime;

	private Integer lifeSpan;

	private Set<AssetConfigDTO> configurations;

	private Long assignedUserId;

	private String assignedUserName;

}
