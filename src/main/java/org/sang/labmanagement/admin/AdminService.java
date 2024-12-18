package org.sang.labmanagement.admin;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.OperationNotPermittedException;
import org.sang.labmanagement.user.Role;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.sang.labmanagement.user.instructor.Department;
import org.sang.labmanagement.user.instructor.Instructor;
import org.sang.labmanagement.user.instructor.InstructorRepository;
import org.sang.labmanagement.user.student.Student;
import org.sang.labmanagement.user.student.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final StudentRepository studentRepository;
	private final InstructorRepository instructorRepository;

	public PageResponse<User>findUsers(int page,int size){
		Pageable pageable= PageRequest.of(page,size);
		Page<User>users=userRepository.findAll(pageable);
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

	public String createUser(CreateUserByAdminRequest request) {
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
		return "Create user successful";
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
		user.setEnabled(request.isEnable());
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

	public String deleteUser(Long id){
		User user=userRepository.findById(id).orElseThrow(
				()->new OperationNotPermittedException("User not found")
		);
		if (user.getRole() == Role.STUDENT && user.getStudent() != null) {
			studentRepository.delete(user.getStudent());
		} else if (user.getRole() == Role.TEACHER && user.getInstructor() != null) {
			instructorRepository.delete(user.getInstructor());
		}
		userRepository.delete(user);
		return "Delete user successful";
	}
}
