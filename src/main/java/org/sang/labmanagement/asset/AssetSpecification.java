package org.sang.labmanagement.asset;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;

public class AssetSpecification {
	public static Specification<Asset> getAssetsByKeywordAndRoom(String keyword, String roomName, String statuses, String categoryIds) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			var configJoin = (keyword != null && !keyword.isEmpty())
					? root.join("configurations", jakarta.persistence.criteria.JoinType.LEFT)
					: null;

			// Lọc theo keyword (name, description, specKey, specValue)
			if (keyword != null && !keyword.isEmpty()) {
				String pattern = "%" + keyword.toLowerCase() + "%";

				Predicate name = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern);
				Predicate description = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern);

				List<Predicate> keywordPredicates = new ArrayList<>();
				keywordPredicates.add(name);
				keywordPredicates.add(description);

				if (configJoin != null) {
					Predicate specKey = criteriaBuilder.like(criteriaBuilder.lower(configJoin.get("specKey")), pattern);
					Predicate specValue = criteriaBuilder.like(criteriaBuilder.lower(configJoin.get("specValue")), pattern);
					keywordPredicates.add(specKey);
					keywordPredicates.add(specValue);
					query.distinct(true);
				}

				predicates.add(criteriaBuilder.or(keywordPredicates.toArray(new Predicate[0])));
			}
			// Lọc theo roomName
			if (roomName != null && !roomName.isEmpty()) {
				predicates.add(criteriaBuilder.like(
						criteriaBuilder.lower(root.get("room").get("name")),
						"%" + roomName.toLowerCase() + "%"
				));
			}

			// Lọc theo statuses (multiple)
			if (statuses != null && !statuses.isEmpty()) {
				List<String> statusList = List.of(statuses.split(","));
				predicates.add(root.get("status").in(statusList));
			}

			// Lọc theo categoryIds (multiple)
			if (categoryIds != null && !categoryIds.isEmpty()) {
				List<Long> categoryIdList = List.of(categoryIds.split(",")).stream()
						.map(Long::parseLong)
						.collect(Collectors.toList());
				predicates.add(root.get("category").get("id").in(categoryIdList));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
