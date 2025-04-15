package org.sang.labmanagement.asset;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.*;
import org.sang.labmanagement.asset.category.Category;
import org.sang.labmanagement.asset.configuration.AssetConfiguration;
import org.sang.labmanagement.asset.location.Location;
import org.sang.labmanagement.room.Room;
import org.sang.labmanagement.user.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Tránh lỗi proxy Hibernate
public class Asset {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AssetStatus status;

	private LocalDateTime purchaseDate;
	private Double price;

	@Column(length = 1000)
	private String image;

	@ManyToOne
	@JoinColumn(name = "category_id")
	@JsonIgnore // Tránh lỗi `ByteBuddyInterceptor`
	private Category category;

	@ManyToOne
	@JoinColumn(name = "location_id")
	@JsonIgnore
	private Location location;

	@ManyToOne
	@JoinColumn(name = "assigned_user_id")
	@JsonBackReference
	private User assignedUser;

	@ManyToOne
	@JoinColumn(name = "room_id")
	@JsonIgnore
	private Room room;

	private Integer quantity;
	private Integer warranty;
	private Integer operationYear;
	private LocalDate operationStartDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OperationTime operationTime;

	private Integer lifeSpan;

	@OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JsonManagedReference
	private Set<AssetConfiguration> configurations;


	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createDate;

	@LastModifiedDate
	@Column(insertable = false)
	private LocalDateTime lastModifiedDate;

	public Integer getRemainingLifeSpan() {
		if (operationYear == null || lifeSpan == null) return null;
		int currentYear = LocalDateTime.now().getYear();
		int yearsUsed = currentYear - operationYear;
		return Math.max(lifeSpan - yearsUsed, 0);
	}
}
