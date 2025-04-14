package org.sang.labmanagement.asset.borrow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AssetBorrowingDTO {

	private Long id;

	private Long assetId;

	private String assetName;

	private Long userId;

	private String username;

	private LocalDateTime borrowDate;

	private LocalDateTime returnDate;

	private LocalDateTime expectedReturnDate;

	private BorrowingStatus status;

	private String remarks;

}
