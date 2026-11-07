package com.blinkweb.library.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for sending security and verification emails via JavaMailSender.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@blinkweb.io}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends the 6-digit OTP code to the requested email.
     * Subject: "BlinkWeb Security Code: [OTP]"
     * @return true if email was accepted by SMTP server, false otherwise.
     */
    public boolean sendOtpEmail(String toEmail, String otp) {
        log.info("Sending security OTP email to [{}] with code [{}]", toEmail, otp);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail.trim());
            message.setSubject("BlinkWeb Security Code: " + otp);
            message.setText(String.format(
                    "Hello,\n\n" +
                    "Your BlinkWeb security verification code is:\n\n" +
                    "    %s\n\n" +
                    "This code will expire in 5 minutes.\n\n" +
                    "If you did not request this verification code, please disregard this email.\n\n" +
                    "Best regards,\n" +
                    "The BlinkWeb Security Team",
                    otp
            ));

            mailSender.send(message);
            log.info("✅ Security email successfully delivered to [{}] via SMTP", toEmail);
            return true;
        } catch (MailException ex) {
            log.error("❌ Failed to send email via SMTP to [{}]: {}. " +
                      "[DEV FALLBACK: Valid OTP is '{}']", toEmail, ex.getMessage(), otp);
            return false;
        }
    }
}
