package org.sang.labmanagement.room;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.asset.software.Software;
import org.sang.labmanagement.asset.software.SoftwareRepository;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.ResourceAlreadyExistsException;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RoomService {
	private final RoomRepository roomRepository;
	private final SoftwareRepository softwareRepository;

	public PageResponse<Room> getAllRooms(int page,int size){
		Pageable pageable= PageRequest.of(page,size);
		Page<Room> rooms=roomRepository.findAll(pageable);
		PageResponse<Room> response=new PageResponse<>();
		response.setContent(rooms.getContent());
		response.setNumber(rooms.getNumber());
		response.setSize(rooms.getSize());
		response.setTotalElements(rooms.getTotalElements());
		response.setTotalPages(rooms.getTotalPages());
		response.setFirst(rooms.isFirst());
		response.setLast(rooms.isLast());
		return response;
	}

	public Room createRoom(RoomDTO roomDTO){
		if(roomRepository.existsByName(roomDTO.getName())){
			throw new ResourceAlreadyExistsException("Room already exists with name :"+roomDTO.getName());
		}
		Room room=new Room();
		room.setName(roomDTO.getName());
		room.setLocation(room.getLocation());
		room.setCapacity(room.getCapacity());
		room.setStatus(roomDTO.getStatus());
		room.setSoftwareList(getSoftwareListByIds(roomDTO.getSoftwareIds()));
		return roomRepository.save(room);
	}

	public Room getRoomById(Long id){
		return roomRepository.findById(id)
				.orElseThrow(()-> new ResourceNotFoundException("Room not found with id :"+id));
	}

	public Room updateRoom(Long id,RoomDTO roomDTO){
		Room room = roomRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

		room.setName(roomDTO.getName());
		room.setLocation(roomDTO.getLocation());
		room.setCapacity(roomDTO.getCapacity());
		room.setStatus(roomDTO.getStatus());
		room.setSoftwareList(getSoftwareListByIds(roomDTO.getSoftwareIds())); // Cập nhật phần mềm

		return roomRepository.save(room);
	}

	public void deleteRoom(Long id){
		Room room=getRoomById(id);
		roomRepository.delete(room);
	}

	private Set<Software> getSoftwareListByIds(List<Long> softwareIds) {
		if (softwareIds == null || softwareIds.isEmpty()) {
			return new HashSet<>();
		}
		return new HashSet<>(softwareRepository.findAllById(softwareIds));
	}


}
