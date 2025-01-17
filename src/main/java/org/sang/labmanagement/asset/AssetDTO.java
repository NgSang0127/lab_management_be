package org.sang.labmanagement.asset;


import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import lombok.Data;


@Data
public class AssetDTO {

	private Long id;

	@NotBlank(message = "Asset name is required")
	private String name;

	private String description;

	private String image;

	@NotBlank(message = "Serial number is required")
	private String serialNumber;

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

	private Long assignedUserId;

	private String assignedUserName;

}
