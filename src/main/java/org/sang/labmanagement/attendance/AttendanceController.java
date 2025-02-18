package org.sang.labmanagement.attendance;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.attendance.request.LocationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

	private final double LAB_LAT=10.877858726886524;
	private final double LAB_LON=106.80155522239467;
	private static double RADIUS=100; //Bán kính 100m

	private final AttendanceService attendanceService;

	@PostMapping()
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> checkAttendance(@RequestBody LocationRequest request,
			Authentication connectedUser){
		double distance=attendanceService.calculateDistance(LAB_LAT,LAB_LON,request.getLatitude(),
				request.getLongitude());

		if(distance <=RADIUS){
			attendanceService.saveAttendanceLog(request,connectedUser);
			return ResponseEntity.ok("Attendance confirmed");
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are outside the allowed area");
	}



}
