package org.sang.labmanagement.asset;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class AssetSpecification {
	public static Specification<Asset> getAssetsByKeywordAndStatus(String keyword, String status) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (keyword != null && !keyword.isEmpty()) {
				String pattern = "%" + keyword.toLowerCase() + "%";
				Predicate name = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern);
				Predicate description = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern);
				predicates.add(criteriaBuilder.or(name, description));
			}

			if (status != null && !status.isEmpty()) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
