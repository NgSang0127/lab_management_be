package org.sang.labmanagement.course;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class CourseSpecification {

	public static Specification<Course> getCoursesByKeyword(String keyword){
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (keyword != null && !keyword.isEmpty()) {
				String pattern = "%" + keyword.toLowerCase() + "%";
				Predicate codePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern);
				Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern);
				Predicate nhPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("NH")), pattern);
				Predicate thPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("TH")), pattern);
				predicates.add(criteriaBuilder.or(codePredicate, namePredicate, nhPredicate, thPredicate));
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}

}
