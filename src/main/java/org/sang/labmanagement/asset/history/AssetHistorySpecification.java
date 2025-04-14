package org.sang.labmanagement.asset.history;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sang.labmanagement.asset.Asset;
import org.sang.labmanagement.asset.AssetStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class AssetHistorySpecification {
	public static Specification<AssetHistory> filterByKeywordAndStatuses(String keyword, String statuses) {
		return (Root<AssetHistory> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

			Predicate finalPredicate = cb.conjunction();

			// Lọc theo keyword trên assetName
			if (StringUtils.hasText(keyword)) {
				Join<AssetHistory, Asset> assetJoin = root.join("asset", JoinType.INNER);
				Predicate keywordPredicate = cb.like(
						cb.lower(assetJoin.get("name")),
						"%" + keyword.trim().toLowerCase() + "%"
				);
				finalPredicate = cb.and(finalPredicate, keywordPredicate);
			}

			// Lọc theo statuses (previousStatus hoặc newStatus)
			if (StringUtils.hasText(statuses)) {
				List<AssetStatus> statusList;
				try {
					statusList = Arrays.stream(statuses.split(","))
							.map(String::trim)
							.filter(StringUtils::hasText)
							.map(status -> AssetStatus.valueOf(status.toUpperCase()))
							.collect(Collectors.toList());
				} catch (IllegalArgumentException e) {
					statusList = List.of();
				}

				if (!statusList.isEmpty()) {
					Predicate statusPredicate = cb.or(
							root.get("previousStatus").in(statusList),
							root.get("newStatus").in(statusList)
					);
					finalPredicate = cb.and(finalPredicate, statusPredicate);
				}
			}

			return finalPredicate;
		};
	}
}
