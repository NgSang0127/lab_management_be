//package org.sang.labmanagement.security.email;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//
//import jakarta.persistence.Table;
//import java.time.LocalDateTime;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.sang.labmanagement.user.User;
//
//@Getter
//@Setter
//@Builder
//@AllArgsConstructor
//@NoArgsConstructor
//@Entity
//@Table(name = "verification_code")
//public class EmailVerificationCode {
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	private Long id;
//
//	private String code;
//
//	private LocalDateTime createdAt;
//
//	private LocalDateTime expiresAt;
//
//	private LocalDateTime validatedAt;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "user_id")
//	private User user;
//
//	public boolean isExpired() {
//		return expiresAt.isBefore(LocalDateTime.now());
//	}
//
//	// Kiểm tra mã có được xác nhận chưa
//	public boolean isValidated() {
//		return validatedAt != null;
//	}
//
//	// Xác nhận mã và lưu thời gian xác nhận
//	public void validate() {
//		this.validatedAt = LocalDateTime.now();
//	}
//
//}
