package org.sang.labmanagement.asset.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetHistoryRepository extends JpaRepository<AssetHistory,Long>, JpaSpecificationExecutor<AssetHistory> {
	Page<AssetHistory> findByAssetId(Long assetId, Pageable pageable);

}
