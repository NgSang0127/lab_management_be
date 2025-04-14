package org.sang.labmanagement.asset.borrow;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetBorrowingRepository extends JpaRepository<AssetBorrowing,Long>, JpaSpecificationExecutor<AssetBorrowing> {
	List<AssetBorrowing> findByUserId(Long userId);

	@Query("SELECT b FROM AssetBorrowing b WHERE b.status = :status AND b.expectedReturnDate < :date")
	List<AssetBorrowing> findAllByStatusAndExpectedReturnDateBefore(BorrowingStatus status, LocalDateTime date);
}
