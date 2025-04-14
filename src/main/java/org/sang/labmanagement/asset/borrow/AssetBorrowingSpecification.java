package org.sang.labmanagement.asset.borrow;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.sang.labmanagement.asset.Asset;
import org.sang.labmanagement.user.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AssetBorrowingSpecification {

	public static Specification<AssetBorrowing> filterByKeywordAndStatus(String keyword, String statuses) {
		return (root, query, cb) -> {
			// Join với Asset và User
			Join<AssetBorrowing, Asset> assetJoin = root.join("asset", JoinType.LEFT);
			Join<AssetBorrowing, User> userJoin = root.join("user", JoinType.LEFT);

			// Xử lý keyword
			String likeKeyword = StringUtils.hasText(keyword) ? "%" + keyword.toLowerCase().trim() + "%" : "%";
			var keywordPredicate = cb.or(
					cb.like(cb.lower(assetJoin.get("name")), likeKeyword),
					cb.like(cb.lower(userJoin.get("username")), likeKeyword)
			);

			// Xử lý statuses
			if (StringUtils.hasText(statuses)) {
				List<String> statusList = Arrays.stream(statuses.split(","))
						.map(String::trim)
						.filter(StringUtils::hasText)
						.collect(Collectors.toList());

				if (!statusList.isEmpty()) {
					if (statusList.size() == 1) {
						String status = statusList.get(0);
						try {
							BorrowingStatus borrowingStatus = BorrowingStatus.valueOf(status.toUpperCase());
							var statusPredicate = cb.equal(root.get("status"), borrowingStatus);
							return cb.and(keywordPredicate, statusPredicate);
						} catch (IllegalArgumentException e) {
							return keywordPredicate;
						}
					} else {
						try {
							List<BorrowingStatus> validStatuses = statusList.stream()
									.map(status -> {
										try {
											return BorrowingStatus.valueOf(status.toUpperCase());
										} catch (IllegalArgumentException e) {
											return null;
										}
									})
									.filter(status -> status != null)
									.collect(Collectors.toList());

							if (!validStatuses.isEmpty()) {
								var statusPredicate = root.get("status").in(validStatuses);
								return cb.and(keywordPredicate, statusPredicate);
							}
						} catch (Exception e) {
							// Nếu có lỗi khi chuyển đổi trạng thái, bỏ qua lọc trạng thái
							return keywordPredicate;
						}
					}
				}
			}

			return keywordPredicate;
		};
	}
}