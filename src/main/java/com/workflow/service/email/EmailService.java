package com.workflow.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${password-reset.token.expiration-minutes}")
    private int expiryMinutes;

    /**
     * Send password reset email with verification code asynchronously
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String username, String verificationCode) {
        try {
            log.info("Sending password reset email to: {}", toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email properties
            helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
            helper.setTo(toEmail);
            helper.setSubject("Reset Your Password - WorkFlow App");

            // Prepare template context
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("expiryMinutes", expiryMinutes);

            // Generate HTML content from template
            String htmlContent = templateEngine.process("email/password-reset", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            // Don't throw exception - email failure shouldn't break the flow
        }
    }
}
