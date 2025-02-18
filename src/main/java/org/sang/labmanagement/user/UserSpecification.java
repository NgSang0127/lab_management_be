package org.sang.labmanagement.user;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

	public static Specification<User> getUsersByKeywordAndRole(String keyword, String role,String currentUsername) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (keyword != null && !keyword.isEmpty()) {
				String pattern = "%" + keyword.toLowerCase() + "%";
				Predicate firstNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), pattern);
				Predicate lastNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), pattern);
				Predicate usernamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern);
				predicates.add(criteriaBuilder.or(firstNamePredicate, lastNamePredicate, usernamePredicate));
			}

			if (role != null && !role.isEmpty()) {
				predicates.add(criteriaBuilder.equal(root.get("role"), role));
			}

			//Loại bỏ ng dùng đang hien tai
			if(currentUsername!= null && !currentUsername.isEmpty()){
				predicates.add(criteriaBuilder.notEqual(root.get("username"),currentUsername));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
