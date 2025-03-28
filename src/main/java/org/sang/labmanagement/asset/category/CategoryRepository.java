package org.sang.labmanagement.asset.category;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {
	Optional<Category>findByName(String name);

	boolean existsByName(String name);

	Page<Category>findAll(Pageable pageable);

}
