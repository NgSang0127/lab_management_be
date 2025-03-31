package org.sang.labmanagement.asset.maintenance;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.security.email.EmailService;
import org.sang.labmanagement.security.email.EmailTemplateName;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MaintenanceScheduler {
	private final MaintenanceService maintenanceService;
	private final EmailService emailService;

	// Kiểm tra bảo trì mỗi ngày vào lúc 8 giờ sáng
	@Scheduled(cron = "0 0 8 * * ?")
	public void sendMaintenanceReminders() {
		Locale locale= LocaleContextHolder.getLocale();
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime reminderTime = now.plusDays(3); // Nhắc nhở 3 ngày trước bảo trì

		// Tìm các lịch bảo trì sắp đến trong khoảng từ 2 ngày 23 giờ đến 3 ngày 1 giờ
		LocalDateTime start = reminderTime.minusHours(23);
		LocalDateTime end = reminderTime.plusHours(1);

		List<MaintenanceDTO> maintenances = maintenanceService.getMaintenancesByScheduledDate(start, end);

		for (MaintenanceDTO maintenance : maintenances) {
			String email = maintenanceService.getEmailByAssetId(maintenance.getAssetId());
			String username=maintenanceService.getUserNameByAssetId(maintenance.getAssetId());
			String subject = "Reminder: Upcoming Maintenance for Asset " + maintenance.getAssetId();
			try {
				emailService.sendMaintenanceReminderEmail(
						email,
						username,
						EmailTemplateName.MAINTENANCE_SCHEDULER,
						subject,
						maintenance.getAssetId(),
						maintenanceService.getAssetNameById(maintenance.getAssetId()),
						maintenance.getScheduledDate().toString(),
						maintenance.getRemarks(),
						locale
				);
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
	}
}
