package org.sang.labmanagement.attendance;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.attendance.request.LocationRequest;
import org.sang.labmanagement.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttendanceService {

	private final AttendanceLogRepository attendanceLogRepository;

	public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		double R = 6371000;//Bán kinh trai dat
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
				Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
						Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return R * c;
	}

	public void saveAttendanceLog(LocationRequest request,Authentication connectedUser){
		var user=(User) connectedUser.getPrincipal();
		AttendanceLog attendanceLog=AttendanceLog.builder()
				.userId(user.getId())
				.latitude(request.getLatitude())
				.longitude(request.getLongitude())
				.timestamp(LocalDateTime.now())
				.build();
		attendanceLogRepository.save(attendanceLog);
	}
}
