package org.sang.labmanagement.asset.asset_history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sang.labmanagement.asset.Asset;
import org.sang.labmanagement.asset.AssetStatus;
import org.sang.labmanagement.user.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class AssetHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false,fetch = FetchType.LAZY)
	@JoinColumn(name = "asset_id")
	private Asset asset;

	@ManyToOne(optional = false,fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	@JoinColumn(nullable = false)
	private AssetStatus previousStatus;

	@Enumerated(EnumType.STRING)
	@JoinColumn(nullable = false)
	private AssetStatus newStatus;

	@Column(nullable = false)
	private LocalDateTime changeDate;

	private String remarks;



}
