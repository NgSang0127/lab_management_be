package org.sang.labmanagement.asset.maintenance;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.sang.labmanagement.asset.Asset;
import org.springframework.data.jpa.domain.Specification;

public class MaintenanceSpecification {
	public static Specification<Maintenance> getAssetsByKeywordAndStatus(String keyword, String status) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			Join<Maintenance, Asset> assetJoin = root.join("asset");


			if (keyword != null && !keyword.isEmpty()) {
				String pattern = "%" + keyword.toLowerCase() + "%";
				Predicate name = criteriaBuilder.like(criteriaBuilder.lower(assetJoin.get("name")), pattern);
				Predicate remarks = criteriaBuilder.like(criteriaBuilder.lower(root.get("remarks")), pattern);
				predicates.add(criteriaBuilder.or(name, remarks));
			}

			if (status != null && !status.isEmpty()) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
