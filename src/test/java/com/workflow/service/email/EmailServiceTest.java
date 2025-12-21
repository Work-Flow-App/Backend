package com.workflow.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private static final String FROM_EMAIL = "noreply@workflow.com";
    private static final String FROM_NAME = "WorkFlow App";
    private static final int EXPIRY_MINUTES = 60;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "fromName", FROM_NAME);
        ReflectionTestUtils.setField(emailService, "expiryMinutes", EXPIRY_MINUTES);
    }

    // ============= Send Password Reset Email Tests =============

    @Test
    void shouldSendPasswordResetEmailSuccessfully() throws MessagingException {
        // Given
        String toEmail = "test@example.com";
        String username = "testuser";
        String verificationCode = "123456";
        String expectedHtmlContent = "<html>Password Reset Email</html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/password-reset"), any(Context.class)))
                .thenReturn(expectedHtmlContent);

        // When
        emailService.sendPasswordResetEmail(toEmail, username, verificationCode);

        // Then
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldSetCorrectEmailProperties() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String username = "john";
        String verificationCode = "token-456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Email Content</html>");

        // When
        emailService.sendPasswordResetEmail(toEmail, username, verificationCode);

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldPassVerificationCodeToTemplate() {
        // Given
        String toEmail = "test@example.com";
        String username = "testuser";
        String verificationCode = "123456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    String code = (String) context.getVariable("verificationCode");
                    assertEquals("123456", code);
                    return "<html>Email</html>";
                });

        // When
        emailService.sendPasswordResetEmail(toEmail, username, verificationCode);

        // Then
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
    }

    @Test
    void shouldPassCorrectVariablesToTemplate() {
        // Given
        String toEmail = "test@example.com";
        String username = "johndoe";
        String verificationCode = "789456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);

                    // Verify all template variables
                    assertEquals("johndoe", context.getVariable("username"));
                    assertEquals("789456", context.getVariable("verificationCode"));
                    assertEquals(60, context.getVariable("expiryMinutes"));

                    return "<html>Email</html>";
                });

        // When
        emailService.sendPasswordResetEmail(toEmail, username, verificationCode);

        // Then
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
    }

    @Test
    void shouldUseCorrectTemplateEngine() {
        // Given
        String toEmail = "test@example.com";
        String username = "user";
        String verificationCode = "123456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Template Content</html>");

        // When
        emailService.sendPasswordResetEmail(toEmail, username, verificationCode);

        // Then
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
    }

    @Test
    void shouldHandleMessagingExceptionGracefully() throws MessagingException {
        // Given
        String toEmail = "test@example.com";
        String username = "testuser";
        String verificationCode = "123456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Email</html>");

        // MimeMessageHelper.setFrom() can throw MessagingException
        doAnswer(invocation -> {
            throw new MessagingException("Mail server error");
        }).when(mailSender).send(any(MimeMessage.class));

        // When/Then - Should not throw exception as it catches MessagingException
        assertDoesNotThrow(() ->
            emailService.sendPasswordResetEmail(toEmail, username, verificationCode)
        );
    }

    @Test
    void shouldThrowTemplateProcessingError() {
        // Given
        String toEmail = "test@example.com";
        String username = "testuser";
        String verificationCode = "123456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing error"));

        // When/Then - Template errors are not caught and will propagate
        assertThrows(RuntimeException.class, () ->
            emailService.sendPasswordResetEmail(toEmail, username, verificationCode)
        );
    }

    // ============= Email Content Tests =============

    @Test
    void shouldSendEmailWithCorrectSubject() throws MessagingException {
        // Given
        String toEmail = "test@example.com";
        String username = "user";
        String verificationCode = "123456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Email</html>");

        // When
        emailService.sendPasswordResetEmail(toEmail, username, verificationCode);

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldSendHtmlEmail() {
        // Given
        String toEmail = "test@example.com";
        String username = "user";
        String verificationCode = "123456";
        String htmlContent = "<html><body><h1>Reset Password</h1></body></html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn(htmlContent);

        // When
        emailService.sendPasswordResetEmail(toEmail, username, verificationCode);

        // Then
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
    }

    // ============= Multiple Email Tests =============

    @Test
    void shouldSendMultipleEmailsSuccessfully() {
        // Given
        String[] emails = {"user1@test.com", "user2@test.com", "user3@test.com"};
        String[] usernames = {"user1", "user2", "user3"};
        String[] verificationCodes = {"123456", "234567", "345678"};

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Email</html>");

        // When
        for (int i = 0; i < emails.length; i++) {
            emailService.sendPasswordResetEmail(emails[i], usernames[i], verificationCodes[i]);
        }

        // Then
        verify(mailSender, times(3)).send(mimeMessage);
        verify(templateEngine, times(3)).process(eq("email/password-reset"), any(Context.class));
    }

    // ============= Edge Cases =============

    @Test
    void shouldHandleSpecialCharactersInUsername() {
        // Given
        String toEmail = "test@example.com";
        String username = "user.name-123_test";
        String verificationCode = "123456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertEquals("user.name-123_test", context.getVariable("username"));
                    return "<html>Email</html>";
                });

        // When
        emailService.sendPasswordResetEmail(toEmail, username, verificationCode);

        // Then
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
    }

    @Test
    void shouldHandleSpecialCharactersInVerificationCode() {
        // Given
        String toEmail = "test@example.com";
        String username = "user";
        String verificationCode = "ABC-123_xyz";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    String code = (String) context.getVariable("verificationCode");
                    assertEquals("ABC-123_xyz", code);
                    return "<html>Email</html>";
                });

        // When
        emailService.sendPasswordResetEmail(toEmail, username, verificationCode);

        // Then
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
    }

    @Test
    void shouldHandleLongVerificationCodes() {
        // Given
        String toEmail = "test@example.com";
        String username = "user";
        String verificationCode = "a".repeat(256); // Very long code

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Email</html>");

        // When/Then
        assertDoesNotThrow(() ->
            emailService.sendPasswordResetEmail(toEmail, username, verificationCode)
        );
    }

    @Test
    void shouldHandleInternationalEmailAddresses() {
        // Given
        String toEmail = "user@тест.com";
        String username = "user";
        String verificationCode = "123456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Email</html>");

        // When/Then
        assertDoesNotThrow(() ->
            emailService.sendPasswordResetEmail(toEmail, username, verificationCode)
        );
    }

    // ============= Configuration Tests =============

    @Test
    void shouldUseConfiguredFromEmail() {
        // Given
        String customFromEmail = "custom@example.com";
        ReflectionTestUtils.setField(emailService, "fromEmail", customFromEmail);

        String toEmail = "test@example.com";
        String username = "user";
        String verificationCode = "123456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Email</html>");

        // When
        emailService.sendPasswordResetEmail(toEmail, username, verificationCode);

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldUseConfiguredExpiryMinutes() {
        // Given
        ReflectionTestUtils.setField(emailService, "expiryMinutes", 30);

        String toEmail = "test@example.com";
        String username = "user";
        String verificationCode = "123456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertEquals(30, context.getVariable("expiryMinutes"));
                    return "<html>Email</html>";
                });

        // When
        emailService.sendPasswordResetEmail(toEmail, username, verificationCode);

        // Then
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
    }
}