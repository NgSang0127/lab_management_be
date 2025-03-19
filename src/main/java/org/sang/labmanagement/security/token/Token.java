//package org.sang.labmanagement.security.token;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.EntityListeners;
//import jakarta.persistence.EnumType;
//import jakarta.persistence.Enumerated;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//import java.time.LocalDateTime;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.sang.labmanagement.user.User;
//import org.springframework.data.annotation.CreatedDate;
//import org.springframework.data.annotation.LastModifiedDate;
//import org.springframework.data.jpa.domain.support.AuditingEntityListener;
//
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Entity
//@Builder
//@EntityListeners(AuditingEntityListener.class)
//public class Token {
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	private Long id;
//
//	@Column(unique = true,length = 512)
//	private String token;
//
//
//	@Enumerated(EnumType.STRING)
//	private TokenType tokenType=TokenType.BEARER;
//
//	private boolean expired;
//
//	private boolean revoked;
//
//	@CreatedDate
//	@Column(nullable = false,updatable = false)
//	private LocalDateTime createdDate;
//
//	@LastModifiedDate
//	@Column(insertable = false)
//	private LocalDateTime lastModifiedDate;
//
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "user_id")
//	private User user;
//
//
//
//}
