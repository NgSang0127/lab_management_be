package org.sang.labmanagement.security.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.user.User;
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
	private final EmailVerificationRepository emailVerificationRepository;


	@Async//public mới duoc proxy quản lý trong spring AOP
	public void sendEmail(
			String to,
			String username,
			EmailTemplateName emailTemplate,
			String confirmationUrl,
			String activationCode,
			String subject
	) throws MessagingException {
		String templateName;
		if(emailTemplate == null){
			templateName="confirm-email";
		}else{
			templateName = emailTemplate.name();
		}

		MimeMessage mimeMessage= mailSender.createMimeMessage();
		MimeMessageHelper helper=new MimeMessageHelper(
				mimeMessage,
				MimeMessageHelper.MULTIPART_MODE_MIXED,
				StandardCharsets.UTF_8.name()
		);
		Map<String,Object>properties= new HashMap<String,Object>();
		properties.put("username",username);
		properties.put("confirmationUrl",confirmationUrl);
		properties.put("activation_code",activationCode);

		Context context=new Context();
		context.setVariables(properties);

		helper.setFrom("contact@admin.com");
		helper.setTo(to);
		helper.setSubject(subject);

		String template=templateEngine.process(templateName,context);
		helper.setText(template,true);

		mailSender.send(mimeMessage);
	}

	public String generateAndSaveActivationCode(User user){
		String generateCodeEmail=generateActivateCode(6);
		var code=EmailVerificationCode.builder()
				.code(generateCodeEmail)
				.createdAt(LocalDateTime.now())
				.expiresAt(LocalDateTime.now().plusMinutes(15))
				.user(user)
				.build();
		emailVerificationRepository.save(code);
		return generateCodeEmail;
	}

	private String generateActivateCode(int length){
		String characters="0123456789";
		StringBuilder codeBuilder = new StringBuilder();
		SecureRandom secureRandom=new SecureRandom();
		for(int i=0; i<length;i++){
			int randomIndex=secureRandom.nextInt(characters.length());
			codeBuilder.append(characters.charAt(randomIndex));
		}
		return codeBuilder.toString();
	}


	@Async
	public void sendMaintenanceReminderEmail(String to, String username, String subject,
			Long assetId, String assetName,
			String scheduledDate, String remarks) throws MessagingException {
		Context context = new Context();
		context.setVariable("username", username);
		context.setVariable("assetId", assetId);
		context.setVariable("assetName", assetName);
		context.setVariable("scheduledDate", scheduledDate);
		context.setVariable("remarks", remarks);

		String htmlContent = templateEngine.process("maintenance-reminder", context);

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
		helper.setTo(to);
		helper.setSubject(subject);
		helper.setText(htmlContent, true);

		mailSender.send(message);
	}
}

