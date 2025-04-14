package org.sang.labmanagement.room;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoomDTO {
	private String name;
	private String location;
	private int capacity;
	private RoomStatus status;
	private List<Long> softwareIds;
}
