package org.sang.labmanagement.room;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room,Long> {

	Room findByName(String name);

	boolean existsByName(String name);
}
