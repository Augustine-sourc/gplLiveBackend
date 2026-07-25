package com.augustine.gplfantasyleaague.domain.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Deliberately swallows send failures rather than propagating them -
    // AuthService calls this as a best-effort step during registration/resend
    // so a flaky SMTP connection doesn't turn into a 500 that blocks account
    // creation entirely. If sending genuinely fails, the user still has
    // "resend code" to retry once mail is working.
    public void sendVerificationCode(String toEmail, String code) {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("MAIL_USERNAME is not configured - skipping verification email to {} (code: {})", toEmail, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Your GPL Live verification code");
            message.setText(
                    "Your GPL Live verification code is: " + code + "\n\n" +
                    "This code expires in 15 minutes. If you didn't request this, you can ignore this email."
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", toEmail, e);
        }
    }
}
