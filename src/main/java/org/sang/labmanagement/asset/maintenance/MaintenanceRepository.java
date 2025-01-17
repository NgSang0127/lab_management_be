package org.sang.labmanagement.asset.maintenance;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance,Long>, JpaSpecificationExecutor<Maintenance> {
	List<Maintenance> findByScheduleDateBetween(LocalDateTime start, LocalDateTime end);
	List<Maintenance> findByStatus(MaintenanceStatus status);

}
