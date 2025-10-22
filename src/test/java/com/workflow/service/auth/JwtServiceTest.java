package com.workflow.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUser;
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"; // Base64 encoded 256-bit key

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inject the secret key using reflection since it's @Value annotated
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", TEST_SECRET);

        // Create test user
        testUser = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    // ============= Token Generation Tests =============

    @Test
    void shouldGenerateValidJwtToken() {
        // When
        String token = jwtService.generateToken(testUser);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void shouldIncludeUsernameInTokenSubject() {
        // When
        String token = jwtService.generateToken(testUser);
        String extractedUsername = jwtService.extractUserName(token);

        // Then
        assertEquals("testuser", extractedUsername);
    }

    @Test
    void shouldSetCorrectExpirationTime() {
        // When
        String token = jwtService.generateToken(testUser);
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);
        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);

        // Then
        assertNotNull(expiration);
        assertNotNull(issuedAt);

        // Token should expire 30 seconds after issue (as per JwtService implementation)
        long expectedExpiration = issuedAt.getTime() + 30 * 1000;
        assertEquals(expectedExpiration, expiration.getTime());
    }

    // ============= Token Validation Tests =============

    @Test
    void shouldValidateTokenWithCorrectUsername() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        boolean isValid = jwtService.isTokenValid(token, testUser);

        // Then
        assertTrue(isValid);
    }

    @Test
    void shouldRejectTokenWithWrongUsername() {
        // Given
        String token = jwtService.generateToken(testUser);
        UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        // Given
        String token = jwtService.generateToken(testUser);

        // Wait for token to expire (30 seconds + buffer)
        // For testing purposes, we'll create an already expired token
        String expiredToken = createExpiredToken(testUser);

        // When/Then
        assertThrows(ExpiredJwtException.class, () -> {
            jwtService.extractUserName(expiredToken);
        });
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() {
        // Given - create token with different secret
        JwtService differentJwtService = new JwtService();
        ReflectionTestUtils.setField(differentJwtService, "SECRET_KEY",
            "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437"); // Different key
        String tokenWithDifferentSignature = differentJwtService.generateToken(testUser);

        // When/Then
        assertThrows(SignatureException.class, () -> {
            jwtService.extractUserName(tokenWithDifferentSignature);
        });
    }

    // ============= Token Extraction Tests =============

    @Test
    void shouldExtractUsernameFromValidToken() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        String username = jwtService.extractUserName(token);

        // Then
        assertEquals("testuser", username);
    }

    @Test
    void shouldExtractExpirationDateFromValidToken() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date())); // Should be in the future
    }

    @Test
    void shouldThrowExceptionForMalformedToken() {
        // Given
        String malformedToken = "this.is.not.a.valid.jwt";

        // When/Then
        assertThrows(MalformedJwtException.class, () -> {
            jwtService.extractUserName(malformedToken);
        });
    }

    @Test
    void shouldThrowExceptionForNullToken() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.extractUserName(null);
        });
    }

    @Test
    void shouldThrowExceptionForEmptyToken() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.extractUserName("");
        });
    }

    @Test
    void shouldExtractCustomClaimsFromToken() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        String subject = jwtService.extractClaim(token, Claims::getSubject);
        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);

        // Then
        assertEquals("testuser", subject);
        assertNotNull(issuedAt);
        assertTrue(issuedAt.before(new Date()) || issuedAt.equals(new Date()));
    }

    // ============= Helper Methods =============

    /**
     * Creates an expired token for testing purposes
     */
    private String createExpiredToken(UserDetails user) {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date(System.currentTimeMillis() - 60000)) // 1 minute ago
                .expiration(new Date(System.currentTimeMillis() - 30000)) // Expired 30 seconds ago
                .signWith(key)
                .compact();
    }
}