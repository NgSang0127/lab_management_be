package org.sang.labmanagement.admin;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.OperationNotPermittedException;
import org.sang.labmanagement.user.Role;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.sang.labmanagement.user.UserSpecification;
import org.sang.labmanagement.user.instructor.Department;
import org.sang.labmanagement.user.instructor.Instructor;
import org.sang.labmanagement.user.instructor.InstructorRepository;
import org.sang.labmanagement.user.student.Student;
import org.sang.labmanagement.user.student.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final StudentRepository studentRepository;
	private final InstructorRepository instructorRepository;

	public PageResponse<User>findUsers(int page,int size,String keyword,String role, Authentication connectedUser){
		var user=(User)connectedUser.getPrincipal();
		Pageable pageable= PageRequest.of(page,size);
		Specification<User> spec = UserSpecification.getUsersByKeywordAndRole(keyword, role,user.getUsername());

		Page<User> users = userRepository.findAll(spec, pageable);
		return PageResponse.<User>builder()
				.content(users.getContent())
				.number(users.getNumber())
				.size(users.getSize())
				.totalElements(users.getTotalElements())
				.totalPages(users.getTotalPages())
				.first(users.isFirst())
				.last(users.isLast())
				.build();
	}

	public User createUser(CreateUserByAdminRequest request) {
		User user = User.builder()
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.email(request.getEmail())
				.username(request.getUsername())
				.password(passwordEncoder.encode(request.getUsername()))
				.role(Role.valueOf(request.getRole().toUpperCase()))
				.phoneNumber(request.getPhoneNumber())
				.enabled(true)
				.accountLocked(false)
				.build();

		User savedUser = userRepository.save(user);
		if (user.getRole() == Role.STUDENT) {
			Student student = Student.builder()
					.user(savedUser)
					.studentId(user.getUsername())
					.build();
			studentRepository.save(student);
		} else if (user.getRole() == Role.TEACHER) {
			Instructor instructor = Instructor.builder()
					.user(savedUser)
					.instructorId(user.getUsername())
					.department(Department.IT)
					.build();
			instructorRepository.save(instructor);
		}
		return savedUser;
	}

	public User updateUser(Long id, CreateUserByAdminRequest request) {
		User user = userRepository.findById(id).orElseThrow(
				() -> new OperationNotPermittedException("User not found")
		);
		user.setFirstName(request.getFirstName());
		user.setLastName(request.getLastName());
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setRole(Role.valueOf(request.getRole().toUpperCase()));
		user.setPhoneNumber(request.getPhoneNumber());
		user.setEnabled(request.isEnabled());
		user.setAccountLocked(request.isAccountLocked());

		if (Role.valueOf(request.getRole().toUpperCase()) != user.getRole()) {
			if (user.getRole() == Role.STUDENT && user.getStudent() != null) {
				studentRepository.delete(user.getStudent());
			} else if (user.getRole() == Role.TEACHER && user.getInstructor() != null) {
				instructorRepository.delete(user.getInstructor());
			}
			if (Role.valueOf(request.getRole().toUpperCase()) == Role.STUDENT) {
				Student student = Student.builder()
						.studentId(request.getUsername())
						.user(user)
						.build();
				studentRepository.save(student);
			} else if (Role.valueOf(request.getRole().toUpperCase()) == Role.TEACHER) {
				Instructor instructor=Instructor.builder()
						.instructorId(request.getUsername())
						.user(user)
						.build();
				instructorRepository.save(instructor);
			}
		}
		return userRepository.save(user);
	}


	public boolean canModifyUser(Role currentRole, Role targetRole) {
		// Owner hoặc Co-Owner có thể chỉnh sửa/xóa Admin, Teacher, Student nhưng không chỉnh sửa lẫn nhau
		if ((currentRole == Role.OWNER || currentRole == Role.CO_OWNER) && targetRole.ordinal() < currentRole.ordinal()) {
			return true;
		}

		// Admin chỉ có thể chỉnh sửa/xóa Teacher hoặc Student
		if (currentRole == Role.ADMIN && (targetRole == Role.TEACHER || targetRole == Role.STUDENT)) {
			return true;
		}

		// Nếu không thuộc các điều kiện trên, từ chối quyền
		return false;
	}


	public User deleteUser(Long id, Authentication connectedUser) {
		User userToDelete = userRepository.findById(id).orElseThrow(
				() -> new OperationNotPermittedException("User not found")
		);

		var currentUserDetails = (User) connectedUser.getPrincipal();
		User currentUser = userRepository.findByUsername(currentUserDetails.getUsername()).orElseThrow(
				()->new UsernameNotFoundException("Username not found")
		);

		// Không cho phép xóa ADMIN hoặc OWNER
		if (userToDelete.getRole() == Role.ADMIN || userToDelete.getRole() == Role.OWNER) {
			throw new OperationNotPermittedException("You are not allowed to delete this user!");
		}

		// Không cho phép tự xóa chính mình
		if (userToDelete.getId().equals(currentUser.getId())) {
			throw new OperationNotPermittedException("You cannot delete yourself!");
		}

		// Nếu user là STUDENT hoặc TEACHER, xóa bình thường
		if (userToDelete.getRole() == Role.STUDENT && userToDelete.getStudent() != null) {
			studentRepository.delete(userToDelete.getStudent());
		} else if (userToDelete.getRole() == Role.TEACHER && userToDelete.getInstructor() != null) {
			instructorRepository.delete(userToDelete.getInstructor());
		}

		userRepository.delete(userToDelete);
		return userToDelete;
	}

	@Scheduled(fixedRate = 86400000) // 24 giờ
	public void removeUnverifiedAccounts() {
		LocalDateTime expirationTime = LocalDateTime.now().minusHours(24);
		List<User> unverifiedUsers = userRepository.findUnverifiedUsersBefore(expirationTime);

		for (User user : unverifiedUsers) {
			userRepository.delete(user);
			System.out.println("Deleted unverified account: " + user.getEmail());
		}
	}


}
