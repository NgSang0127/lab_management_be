package org.sang.labmanagement.asset.software;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SoftwareRepository extends JpaRepository<Software,Long> {

	boolean existsBySoftwareName(String softwareName);

	@Query(value = "SELECT s.* FROM software s " +
			"JOIN room_software rs ON s.id = rs.software_id " +
			"WHERE rs.room_id = :roomId", nativeQuery = true)
	List<Software> findByRoomId(@Param("roomId") Long roomId);

}
