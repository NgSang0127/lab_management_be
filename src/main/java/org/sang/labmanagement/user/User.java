package org.sang.labmanagement.user;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
//import org.sang.labmanagement.notification.Notification;
//import org.sang.labmanagement.security.email.EmailVerificationCode;
//import org.sang.labmanagement.security.token.Token;
import org.sang.labmanagement.user.instructor.Instructor;
import org.sang.labmanagement.user.student.Student;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails , Principal {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String firstName;

	private String lastName;

	private String username;

	@JsonProperty(access = Access.WRITE_ONLY)
	private String password;

	private String phoneNumber;


	@Column(unique = true)
	private String email;

	@Enumerated(EnumType.STRING)
	private Role role;

	@Column(nullable = false)
	private boolean twoFactorEnabled;

	private String secret;

	private boolean accountLocked;

	private boolean enabled;

	@JsonBackReference
	@OneToOne(mappedBy = "user",cascade = CascadeType.ALL)
	private Student student;

	@JsonBackReference
	@OneToOne(mappedBy = "user",cascade = CascadeType.ALL)
	private Instructor instructor;

	@CreatedDate
	@Column(nullable = false,updatable = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
	private LocalDateTime createdDate;

	@LastModifiedDate
	@Column(insertable = false)
	private LocalDateTime lastModifiedDate;



	@Column(length = 1000)
	private String image;

//	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//	private Set<Notification> notifications;



//	@JsonIgnore
//	@OneToMany(mappedBy = "user",cascade = CascadeType.ALL)
//	private List<UserActivity> userActivities;


	public String getFullName(){
		return lastName+" "+firstName;
	}

	@Override
	public String getName() {
		return username;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_"+role.name()));
	}

	@Override
	public boolean implies(Subject subject) {
		return Principal.super.implies(subject);
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !accountLocked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}
	@Override
	public boolean isEnabled() {
		return enabled;
	}

}
