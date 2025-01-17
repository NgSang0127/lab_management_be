package org.sang.labmanagement.asset.location;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location,Long> {

	Page<Location> findAll(Pageable pageable);

	Optional<Location> findByName(String name);

	boolean existsByName(String name);
}
