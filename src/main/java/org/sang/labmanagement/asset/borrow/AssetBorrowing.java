package org.sang.labmanagement.asset.borrow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sang.labmanagement.asset.Asset;
import org.sang.labmanagement.user.User;

@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Builder
@Getter
public class AssetBorrowing {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "asset_id", nullable = false)
	private Asset asset;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "borrow_date")
	private LocalDateTime borrowDate;

	@Column(name = "return_date")
	private LocalDateTime returnDate;

	@Column(name = "expected_return_date", nullable = false)
	private LocalDateTime expectedReturnDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BorrowingStatus status;

	private String remarks;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}
