package org.sang.labmanagement.security.email;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailController {
	private final EmailService emailService;

	@PostMapping("/contact")
	public ResponseEntity<String> sendContactForm(@Valid @RequestBody ContactFormDto form) {
		Locale locale = LocaleContextHolder.getLocale();
		try {
			emailService.sendContactFormEmail(
					form.getName(),
					form.getEmail(),
					form.getMessage(),
					form.getSubject(),
					form.getEmailTemplate(),
					locale
			);
			return ResponseEntity.ok("Message sent successfully");
		} catch (MessagingException e) {
			return ResponseEntity.status(500).body("Error sending email: " + e.getMessage());
		}
	}
}
