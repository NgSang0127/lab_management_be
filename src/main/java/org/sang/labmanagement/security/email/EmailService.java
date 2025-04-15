package org.sang.labmanagement.security.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.redis.BaseRedisServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class EmailService {
	private final JavaMailSender mailSender;
	private final SpringTemplateEngine templateEngine;
	private final BaseRedisServiceImpl<String> redisService;

	@Value("${application.mailing.expiration}")
	private long codeExpiration;

	@Value("${app.email.admin}")
	private String adminEmail;

	// Các phương thức hiện có (giữ nguyên)
	public void saveEmailCode(String email, String code) {
		String existingCode = redisService.get("email_verification:" + email);
		if (existingCode != null) {
			deleteEmailCode(email);
		}
		redisService.setWithExpiration("email_verification:" + email, code, codeExpiration);
	}

	public String getEmailCode(String email) {
		return redisService.get("email_verification:" + email);
	}

	public boolean isEmailCodeMatch(String email, String code) {
		String storedCode = getEmailCode(email);
		return storedCode != null && storedCode.equals(code);
	}

	public void deleteEmailCode(String email) {
		redisService.delete("email_verification:" + email);
	}

	public void revokeEmailCode(String email) {
		redisService.delete("email_verification:" + email);
	}

	public boolean isEmailCodeExpired(String email) {
		Long ttl = redisService.getTTL("email_verification:" + email);
		return ttl == null || ttl == -2;
	}

	@Async
	public void sendEmail(
			String to,
			EmailTemplateName emailTemplate,
			String confirmationUrl,
			String activationCode,
			String subject,
			Locale locale
	) throws MessagingException {
		String templateName = (emailTemplate == null) ? "activate_account" : emailTemplate.getName();
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(
				mimeMessage,
				MimeMessageHelper.MULTIPART_MODE_MIXED,
				StandardCharsets.UTF_8.name()
		);
		Map<String, Object> properties = new HashMap<>();
		properties.put("confirmationUrl", confirmationUrl);
		properties.put("activation_code", activationCode);
		properties.put("email", to);

		createContextEmail(to, subject, templateName, mimeMessage, helper, properties, locale);
	}

	public String generateAndSaveActivationCode(String email) {
		String generateCodeEmail = generateActivateCode(6);
		saveEmailCode(email, generateCodeEmail);
		return generateCodeEmail;
	}

	private String generateActivateCode(int length) {
		String characters = "0123456789";
		StringBuilder codeBuilder = new StringBuilder();
		SecureRandom secureRandom = new SecureRandom();
		for (int i = 0; i < length; i++) {
			int randomIndex = secureRandom.nextInt(characters.length());
			codeBuilder.append(characters.charAt(randomIndex));
		}
		return codeBuilder.toString();
	}

	@Async
	public void sendMaintenanceReminderEmail(
			String to, String username, EmailTemplateName emailTemplate, String subject,
			Long assetId, String assetName,
			String scheduledDate, String remarks, Locale locale
	) throws MessagingException {
		String templateName = (emailTemplate != null) ? emailTemplate.getName() : "maintenance_scheduler";
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(
				mimeMessage,
				MimeMessageHelper.MULTIPART_MODE_MIXED,
				StandardCharsets.UTF_8.name()
		);
		Map<String, Object> properties = new HashMap<>();
		properties.put("username", username);
		properties.put("assetId", assetId);
		properties.put("assetName", assetName);
		properties.put("scheduledDate", scheduledDate);
		properties.put("remarks", remarks);

		createContextEmail(to, subject, templateName, mimeMessage, helper, properties, locale);
	}

	@Async
	public void sendOTPToEmail(
			String to,
			String username,
			EmailTemplateName emailTemplate,
			String activationCode,
			String subject,
			Locale locale
	) throws MessagingException {
		String templateName = emailTemplate.getName();
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(
				mimeMessage,
				MimeMessageHelper.MULTIPART_MODE_MIXED,
				StandardCharsets.UTF_8.name()
		);
		Map<String, Object> properties = new HashMap<>();
		properties.put("username", username);
		properties.put("activation_code", activationCode);

		createContextEmail(to, subject, templateName, mimeMessage, helper, properties, locale);
	}

	@Async
	public void sendOverdueBorrowingEmail(
			String to,
			String username,
			Long borrowingId,
			String assetName,
			LocalDateTime expectedReturnDate,
			String remarks,
			EmailTemplateName emailTemplate,
			String subject,
			Locale locale
	) throws MessagingException {
		String templateName = (emailTemplate != null) ? emailTemplate.getName() : "overdue_borrowing";
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(
				mimeMessage,
				MimeMessageHelper.MULTIPART_MODE_MIXED,
				StandardCharsets.UTF_8.name()
		);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
		String formattedDate = expectedReturnDate.format(formatter);

		Map<String, Object> properties = new HashMap<>();
		properties.put("username", username);
		properties.put("borrowingId", borrowingId);
		properties.put("assetName", assetName);
		properties.put("expectedReturnDate", formattedDate);
		properties.put("remarks", (remarks != null && !remarks.isEmpty()) ? remarks : "None");

		createContextEmail(to, subject, templateName, mimeMessage, helper, properties, locale);
	}


	@Async
	public void sendContactFormEmail(
			String name,
			String email,
			String message,
			String subject,
			EmailTemplateName emailTemplate,
			Locale locale
	) throws MessagingException {
		sendUserConfirmationEmail(name, email, message, subject, emailTemplate, locale);
		sendAdminNotificationEmail(name, email, message, subject, emailTemplate, locale);
	}

	private void sendUserConfirmationEmail(
			String name,
			String email,
			String message,
			String subject,
			EmailTemplateName emailTemplate,
			Locale locale
	) throws MessagingException {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(
				mimeMessage,
				MimeMessageHelper.MULTIPART_MODE_MIXED,
				StandardCharsets.UTF_8.name()
		);
		String templateName = (emailTemplate != null) ? emailTemplate.getName() : "contact_user";

		Map<String, Object> properties = new HashMap<>();
		properties.put("name", name);
		properties.put("email", email);
		properties.put("message", message);
		properties.put("website_url", "https://your-website.com");

		createContextEmail(email, subject != null ? subject : "Thank You for Your Message", templateName, mimeMessage, helper, properties, locale);
	}

	private void sendAdminNotificationEmail(
			String name,
			String email,
			String message,
			String subject,
			EmailTemplateName emailTemplate,
			Locale locale
	) throws MessagingException {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(
				mimeMessage,
				MimeMessageHelper.MULTIPART_MODE_MIXED,
				StandardCharsets.UTF_8.name()
		);
		String templateName = (emailTemplate != null) ? emailTemplate.getName() : "contact_admin";

		Map<String, Object> properties = new HashMap<>();
		properties.put("name", name);
		properties.put("email", email);
		properties.put("message", message);
		properties.put("website_url", "https://your-website.com");

		createContextEmail(adminEmail, subject != null ? "[New Contact] " + subject : "New Contact Form Submission", templateName, mimeMessage, helper, properties, locale);
	}

	private void createContextEmail(
			String to,
			String subject,
			String templateName,
			MimeMessage mimeMessage,
			MimeMessageHelper helper,
			Map<String, Object> properties,
			Locale locale
	) throws MessagingException {
		Context context = new Context(locale);
		context.setVariables(properties);
		helper.setFrom("contact@admin.com");
		helper.setTo(to);
		helper.setSubject(subject);
		String template = templateEngine.process(templateName, context);
		helper.setText(template, true);

		mailSender.send(mimeMessage);
	}
}