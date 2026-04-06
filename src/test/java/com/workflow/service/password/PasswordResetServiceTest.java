package com.workflow.service.password;

import com.workflow.common.constant.Role;
import com.workflow.common.exception.business.InvalidPasswordResetTokenException;
import com.workflow.entity.PasswordResetToken;
import com.workflow.entity.User;
import com.workflow.repository.PasswordResetTokenRepository;
import com.workflow.repository.RefreshTokenRepository;
import com.workflow.repository.UserRepository;
import com.workflow.service.auth.PasswordResetService;
import com.workflow.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private PasswordResetToken validToken;
    private PasswordResetToken expiredToken;
    private PasswordResetToken usedToken;

    private static final int EXPIRATION_MINUTES = 60;
    private static final int MAX_ACTIVE_TOKENS = 3;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "expirationMinutes", EXPIRATION_MINUTES);
        ReflectionTestUtils.setField(passwordResetService, "maxActiveTokensPerUser", MAX_ACTIVE_TOKENS);

        testUser = User.builder()
                .id(1L)
                .uuid("user-uuid-123")
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$encodedPassword")
                .role(Role.WORKER)
                .enabled(true)
                .build();

        validToken = PasswordResetToken.builder()
                .id(1L)
                .verificationCode("valid-token-123")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        expiredToken = PasswordResetToken.builder()
                .id(2L)
                .verificationCode("expired-token-456")
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .used(false)
                .createdAt(LocalDateTime.now().minusMinutes(70))
                .build();

        usedToken = PasswordResetToken.builder()
                .id(3L)
                .verificationCode("used-token-789")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(true)
                .createdAt(LocalDateTime.now())
                .usedAt(LocalDateTime.now())
                .build();
    }

    // ============= Create Password Reset Token Tests =============

    @Test
    void shouldCreatePasswordResetTokenSuccessfully() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.countActiveTokensByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        passwordResetService.createPasswordResetToken(email);

        // Then
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        verify(emailService).sendPasswordResetEmail(eq(email), eq("testuser"), anyString());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getVerificationCode());
        assertEquals(testUser, savedToken.getUser());
        assertFalse(savedToken.isUsed());
        assertNotNull(savedToken.getExpiresAt());
    }

    @Test
    void shouldNotRevealUserExistenceForNonExistentEmail() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When
        passwordResetService.createPasswordResetToken(email);

        // Then
        verify(userRepository).findByEmail(email);
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldGenerateUniqueTokenForEachRequest() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.countActiveTokensByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        passwordResetService.createPasswordResetToken(email);
        passwordResetService.createPasswordResetToken(email);

        // Then
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository, times(2)).save(tokenCaptor.capture());

        List<PasswordResetToken> savedTokens = tokenCaptor.getAllValues();
        assertNotEquals(savedTokens.get(0).getVerificationCode(), savedTokens.get(1).getVerificationCode());
    }

    @Test
    void shouldSetCorrectExpirationTime() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.countActiveTokensByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeCreation = LocalDateTime.now();

        // When
        passwordResetService.createPasswordResetToken(email);

        LocalDateTime afterCreation = LocalDateTime.now();

        // Then
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        LocalDateTime expectedExpiry = beforeCreation.plusMinutes(EXPIRATION_MINUTES);
        LocalDateTime expectedExpiryMax = afterCreation.plusMinutes(EXPIRATION_MINUTES);

        assertTrue(savedToken.getExpiresAt().isAfter(expectedExpiry.minusSeconds(1)));
        assertTrue(savedToken.getExpiresAt().isBefore(expectedExpiryMax.plusSeconds(1)));
    }

    @Test
    void shouldSendEmailWithCorrectParameters() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.countActiveTokensByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        passwordResetService.createPasswordResetToken(email);

        // Then
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendPasswordResetEmail(
                emailCaptor.capture(),
                usernameCaptor.capture(),
                tokenCaptor.capture()
        );

        assertEquals("test@example.com", emailCaptor.getValue());
        assertEquals("testuser", usernameCaptor.getValue());
        assertNotNull(tokenCaptor.getValue());
    }

    @Test
    void shouldDeleteOldestTokenWhenMaxActiveTokensReached() {
        // Given
        String email = "test@example.com";
        PasswordResetToken oldestToken = PasswordResetToken.builder()
                .id(10L)
                .verificationCode("oldest-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .createdAt(LocalDateTime.now().minusMinutes(50))
                .used(false)
                .build();

        PasswordResetToken newerToken1 = PasswordResetToken.builder()
                .id(11L)
                .verificationCode("newer-token-1")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .used(false)
                .build();

        PasswordResetToken newerToken2 = PasswordResetToken.builder()
                .id(12L)
                .verificationCode("newer-token-2")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .used(false)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.countActiveTokensByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(3L); // Max reached
        when(passwordResetTokenRepository.findActiveTokensByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(oldestToken, newerToken1, newerToken2));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        passwordResetService.createPasswordResetToken(email);

        // Then
        verify(passwordResetTokenRepository).delete(oldestToken); // Oldest should be deleted
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class)); // New token saved
    }

    // ============= Reset Password Tests =============

    @Test
    void shouldResetPasswordSuccessfully() {
        // Given
        String email = "test@example.com";
        String code = "valid-token-123";
        String newPassword = "newPassword123";
        String encodedPassword = "$2a$10$encodedNewPassword";

        when(passwordResetTokenRepository.findByVerificationCodeAndUser_Email(code, email))
                .thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        passwordResetService.resetPassword(email, code, newPassword);

        // Then
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
        verify(passwordResetTokenRepository).save(validToken);
        verify(refreshTokenRepository).revokeAllUserTokens(eq(testUser), any(LocalDateTime.class));

        assertEquals(encodedPassword, testUser.getPassword());
        assertTrue(validToken.isUsed());
        assertNotNull(validToken.getUsedAt());
    }

    @Test
    void shouldMarkTokenAsUsedAfterPasswordReset() {
        // Given
        String email = "test@example.com";
        String code = "valid-token-123";
        String newPassword = "newPassword123";

        when(passwordResetTokenRepository.findByVerificationCodeAndUser_Email(code, email))
                .thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertFalse(validToken.isUsed());

        // When
        passwordResetService.resetPassword(email, code, newPassword);

        // Then
        assertTrue(validToken.isUsed());
        assertNotNull(validToken.getUsedAt());
    }

    @Test
    void shouldRevokeAllRefreshTokensAfterPasswordReset() {
        // Given
        String email = "test@example.com";
        String code = "valid-token-123";
        String newPassword = "newPassword123";

        when(passwordResetTokenRepository.findByVerificationCodeAndUser_Email(code, email))
                .thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        passwordResetService.resetPassword(email, code, newPassword);

        // Then
        verify(refreshTokenRepository).revokeAllUserTokens(eq(testUser), any(LocalDateTime.class));
    }

    @Test
    void shouldEncodePasswordBeforeSaving() {
        // Given
        String email = "test@example.com";
        String code = "valid-token-123";
        String newPassword = "myNewPassword";
        String encodedPassword = "$2a$10$encodedMyNewPassword";

        when(passwordResetTokenRepository.findByVerificationCodeAndUser_Email(code, email))
                .thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        passwordResetService.resetPassword(email, code, newPassword);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(encodedPassword, savedUser.getPassword());
        assertNotEquals(newPassword, savedUser.getPassword());
    }

    @Test
    void shouldThrowExceptionWhenResettingWithExpiredToken() {
        // Given
        String email = "test@example.com";
        String code = "expired-token-456";
        String newPassword = "newPassword123";

        when(passwordResetTokenRepository.findByVerificationCodeAndUser_Email(code, email))
                .thenReturn(Optional.of(expiredToken));

        // When/Then
        assertThrows(
                InvalidPasswordResetTokenException.class,
                () -> passwordResetService.resetPassword(email, code, newPassword)
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenRepository, never()).revokeAllUserTokens(any(User.class), any(LocalDateTime.class));
    }

    @Test
    void shouldThrowExceptionWhenResettingWithUsedToken() {
        // Given
        String email = "test@example.com";
        String code = "used-token-789";
        String newPassword = "newPassword123";

        when(passwordResetTokenRepository.findByVerificationCodeAndUser_Email(code, email))
                .thenReturn(Optional.of(usedToken));

        // When/Then
        assertThrows(
                InvalidPasswordResetTokenException.class,
                () -> passwordResetService.resetPassword(email, code, newPassword)
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // ============= Cleanup Expired Tokens Tests =============

    @Test
    void shouldCleanupExpiredTokensSuccessfully() {
        // Given
        when(passwordResetTokenRepository.deleteExpiredAndUsedTokens(any(LocalDateTime.class)))
                .thenReturn(5);

        // When
        int deletedCount = passwordResetService.cleanupExpiredTokens();

        // Then
        assertEquals(5, deletedCount);
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(passwordResetTokenRepository).deleteExpiredAndUsedTokens(cutoffCaptor.capture());

        LocalDateTime cutoff = cutoffCaptor.getValue();
        LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(1);
        assertTrue(cutoff.isBefore(expectedCutoff.plusSeconds(1)));
        assertTrue(cutoff.isAfter(expectedCutoff.minusSeconds(1)));
    }

    @Test
    void shouldReturnZeroWhenNoTokensToCleanup() {
        // Given
        when(passwordResetTokenRepository.deleteExpiredAndUsedTokens(any(LocalDateTime.class)))
                .thenReturn(0);

        // When
        int deletedCount = passwordResetService.cleanupExpiredTokens();

        // Then
        assertEquals(0, deletedCount);
    }

    // ============= Revoke User Tokens Tests =============

    @Test
    void shouldRevokeAllUnusedTokensForUser() {
        // Given
        doNothing().when(passwordResetTokenRepository).deleteUnusedTokensByUser(testUser);

        // When
        passwordResetService.revokeUserTokens(testUser);

        // Then
        verify(passwordResetTokenRepository).deleteUnusedTokensByUser(testUser);
    }

    // ============= Edge Cases =============

    @Test
    void shouldHandleMultipleSimultaneousTokenRequests() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.countActiveTokensByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        for (int i = 0; i < 5; i++) {
            passwordResetService.createPasswordResetToken(email);
        }

        // Then
        verify(passwordResetTokenRepository, times(5)).save(any(PasswordResetToken.class));
        verify(emailService, times(5)).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldHandleLongPasswords() {
        // Given
        String email = "test@example.com";
        String code = "valid-token-123";
        String longPassword = "a".repeat(128);

        when(passwordResetTokenRepository.findByVerificationCodeAndUser_Email(code, email))
                .thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode(longPassword)).thenReturn("encoded-long-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When/Then
        assertDoesNotThrow(() ->
            passwordResetService.resetPassword(email, code, longPassword)
        );
    }

    @Test
    void shouldHandleSpecialCharactersInEmail() {
        // Given
        String email = "test+tag@example.com";
        User userWithSpecialEmail = User.builder()
                .id(2L)
                .username("specialuser")
                .email(email)
                .password("encoded")
                .role(Role.WORKER)
                .enabled(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(userWithSpecialEmail));
        when(passwordResetTokenRepository.countActiveTokensByUser(eq(userWithSpecialEmail), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        passwordResetService.createPasswordResetToken(email);

        // Then
        verify(emailService).sendPasswordResetEmail(eq(email), eq("specialuser"), anyString());
    }

    @Test
    void shouldNotDeleteTokensWhenBelowMaxLimit() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.countActiveTokensByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(1L); // Below max
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        passwordResetService.createPasswordResetToken(email);

        // Then
        verify(passwordResetTokenRepository, never()).delete(any(PasswordResetToken.class));
        verify(passwordResetTokenRepository, never()).findActiveTokensByUser(any(User.class), any(LocalDateTime.class));
    }
}