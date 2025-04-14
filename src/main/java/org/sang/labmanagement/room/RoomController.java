package org.sang.labmanagement.room;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/rooms")
@RequiredArgsConstructor
public class RoomController {
	private final RoomService roomService;

	@GetMapping
	public ResponseEntity<PageResponse<Room>>getAllRooms(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
	){
		return ResponseEntity.ok(roomService.getAllRooms(page,size));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Room> getRoomById(@PathVariable Long id){
		Room room = roomService.getRoomById(id);
		return ResponseEntity.ok(room);
	}

	@PostMapping
	public ResponseEntity<Room> createRoom(@RequestBody RoomDTO room){
		Room createdRoom = roomService.createRoom(room);
		return ResponseEntity.ok(createdRoom);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Room> updateRoom(@PathVariable Long id,@RequestBody RoomDTO room){
		Room updatedRoom=roomService.updateRoom(id, room);
		return ResponseEntity.ok(updatedRoom);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteRoom (@PathVariable Long id){
		roomService.deleteRoom(id);
		return ResponseEntity.ok("Room deleted successfully!");
	}
}
