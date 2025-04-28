package org.sang.labmanagement.semester;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.asset.category.Category;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.ResourceAlreadyExistsException;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SemesterService {
	private final SemesterRepository semesterRepository;


	public PageResponse<Semester> getAllSemester(int page,int size){
		Pageable pageable= PageRequest.of(page,size);
		Page<Semester> semesters=semesterRepository.findAll(pageable);
		return PageResponse.<Semester>builder()
				.content(semesters.getContent())
				.number(semesters.getNumber())
				.size(semesters.getSize())
				.totalElements(semesters.getTotalElements())
				.totalPages(semesters.getTotalPages())
				.first(semesters.isFirst())
				.last(semesters.isLast())
				.build();
	}

	public Semester createSemester(Semester semester){
		Optional<Semester> existingSemester=semesterRepository.findByNameAndAcademicYear(semester.getName(),
				semester.getAcademicYear());
		if(existingSemester.isPresent()){
			throw new ResourceAlreadyExistsException("Semester with name "+semester.getName()+" and academic year "+ semester.getAcademicYear()+" already exists");
		}
		if(semester.getStartDate().isAfter(semester.getEndDate())){
			throw new IllegalArgumentException("Start date must be  before endDate");
		}
		return semesterRepository.save(semester);
	}

	public Semester getSemesterById(Long id){
		return semesterRepository.findById(id)
				.orElseThrow(()-> new ResourceNotFoundException("Semester not found with id :"+id));
	}

	@Transactional
	public Semester updateSemester(Long id,Semester semesterDetails){
		Semester semester=getSemesterById(id);
		semester.setName(semesterDetails.getName());
		semester.setAcademicYear(semesterDetails.getAcademicYear());
		semester.setStartDate(semesterDetails.getStartDate());
		semester.setEndDate(semesterDetails.getEndDate());

		if (semester.getStartDate().isAfter(semester.getEndDate())) {
			throw new IllegalArgumentException("Start date must be before end date");
		}

		return semesterRepository.save(semester);
	}

	@Transactional
	public void deleteSemester(Long id) {
		Semester semester = getSemesterById(id);
		semesterRepository.delete(semester);
	}

	public List<Semester> getActiveSemesters() {
		LocalDate today = LocalDate.now();
		return semesterRepository.findAll().stream()
				.filter(semester -> !semester.getEndDate().isBefore(today))
				.toList();
	}

}
