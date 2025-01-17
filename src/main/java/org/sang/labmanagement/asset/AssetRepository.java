package org.sang.labmanagement.asset;

import java.util.List;
import java.util.Optional;
import org.sang.labmanagement.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<Asset,Long>, JpaSpecificationExecutor<Asset> {
	Optional<Asset>findBySerialNumber(String serialNumber);

	boolean existsBySerialNumber(String serialNumber);

	Page<Asset> findByAssignedUserId(Long userId,Pageable pageable);

	List<Asset> findByStatus(AssetStatus status);


}
