package com.workflow.service.email;

import com.workflow.common.constant.CompanyRole;
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

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${email-verification.token.expiration-hours}")
    private int verificationExpiryHours;

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
            helper.setSubject("Reset Your Password - Workfloow App");

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

    /**
     * Send email verification link asynchronously
     */
    @Async
    public void sendEmailVerificationEmail(String toEmail, String username, String verificationLink, int expirationHours) {
        try {
            log.info("Sending email verification to: {}", toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email - Workfloow App");

            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("verificationLink", verificationLink);
            context.setVariable("expirationHours", expirationHours);

            String htmlContent = templateEngine.process("email/email-verification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email verification sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send email verification to: {}", toEmail, e);
        }
    }

    /**
     * Send worker invitation email with signup link asynchronously
     */
    @Async
    public void sendWorkerInvitationEmail(String toEmail, String companyName, String invitationToken) {
        try {
            log.info("Sending worker invitation email to: {} from company: {}", toEmail, companyName);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email properties
            helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
            helper.setTo(toEmail);
            helper.setSubject("You're Invited to Join " + companyName + " - Workfloow App");

            // Prepare template context
            String signupLink = frontendUrl + "/signup/worker?token=" + invitationToken;
            Context context = new Context();
            context.setVariable("companyName", companyName);
            context.setVariable("signupLink", signupLink);
            context.setVariable("expiryDays", 7);
            context.setVariable("email", toEmail);

            // Generate HTML content from template
            String htmlContent = templateEngine.process("email/worker-invitation", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);
            log.info("Worker invitation email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send worker invitation email to: {}", toEmail, e);
            // Don't throw exception - email failure shouldn't break the flow
        }
    }

    @Async
    public void sendCompanyMemberInvitationEmail(String toEmail, String companyName, CompanyRole companyRole, String invitationToken) {
        try {
            log.info("Sending member invitation email to: {} for company: {}", toEmail, companyName);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
            helper.setTo(toEmail);
            helper.setSubject("You're Invited to Join " + companyName + " - Workfloow App");

            String signupLink = frontendUrl + "/signup/company-member?token=" + invitationToken;
            Context context = new Context();
            context.setVariable("companyName", companyName);
            context.setVariable("companyRole", companyRole.name());
            context.setVariable("signupLink", signupLink);
            context.setVariable("expiryDays", 7);
            context.setVariable("email", toEmail);

            String htmlContent = templateEngine.process("email/member-invitation", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Member invitation email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send member invitation email to: {}", toEmail, e);
        }
    }
}
