package com.workflow.service.auth;

import com.workflow.config.properties.JwtConfigProperties;
import com.workflow.dto.auth.AuthenticationResponse;
import com.workflow.dto.auth.LoginRequest;
import com.workflow.entity.auth.RefreshToken;
import com.workflow.entity.auth.User;
import com.workflow.repository.auth.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtConfigProperties jwtConfigProperties;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private LoginRequest loginRequest;
    private RefreshToken mockRefreshToken;

    @BeforeEach
    void setUp() {
        JwtConfigProperties.AccessToken accessToken = new JwtConfigProperties.AccessToken();
        lenient().when(jwtConfigProperties.getAccessToken()).thenReturn(accessToken);

        testUser = User.builder()
                .id(1L)
                .uuid("test-uuid-123")
                .username("testuser")
                .password("$2a$10$encodedPassword")
                .email("test@example.com")
                .role(com.workflow.common.constant.Role.WORKER)
                .enabled(true)
                .build();

        loginRequest = new LoginRequest("testuser", "password123");

        mockRefreshToken = RefreshToken.builder()
                .id(1L)
                .token("mock-refresh-token-uuid")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();
    }

    // ============= Authentication Tests =============

    @Test
    void shouldAuthenticateUserWithValidCredentials() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // AuthenticationManager returns null on success
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("mock-jwt-token");
        when(refreshTokenService.createRefreshToken(eq(testUser), eq(httpRequest))).thenReturn(mockRefreshToken);

        // When
        AuthenticationResponse response = authenticationService.authenticate(loginRequest, httpRequest);

        // Then
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals("mock-refresh-token-uuid", response.getRefreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
        verify(jwtService).generateToken(testUser);
        verify(refreshTokenService).createRefreshToken(eq(testUser), eq(httpRequest));
    }

    @Test
    void shouldReturnJwtTokenOnSuccessfulAuthentication() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("generated-token-abc123");
        when(refreshTokenService.createRefreshToken(eq(testUser), eq(httpRequest))).thenReturn(mockRefreshToken);

        // When
        AuthenticationResponse response = authenticationService.authenticate(loginRequest, httpRequest);

        // Then
        assertNotNull(response);
        assertEquals("generated-token-abc123", response.getAccessToken());
        assertTrue(response.getAccessToken().length() > 0);
    }

    @Test
    void shouldThrowExceptionForInvalidUsername() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When/Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            authenticationService.authenticate(loginRequest, httpRequest);
        });

        assertEquals("User not found: testuser", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldThrowExceptionForInvalidPassword() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid password"));

        // When/Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authenticationService.authenticate(loginRequest, httpRequest);
        });

        assertEquals("Invalid password", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByUsername(anyString());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldDelegateToAuthenticationManager() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("token");
        when(refreshTokenService.createRefreshToken(eq(testUser), eq(httpRequest))).thenReturn(mockRefreshToken);

        // When
        authenticationService.authenticate(loginRequest, httpRequest);

        // Then
        verify(authenticationManager).authenticate(argThat(token ->
                token instanceof UsernamePasswordAuthenticationToken &&
                token.getPrincipal().equals("testuser") &&
                token.getCredentials().equals("password123")
        ));
    }

    @Test
    void shouldHandleDisabledUser() {
        // Given
        User disabledUser = User.builder()
                .id(1L)
                .uuid("test-uuid-123")
                .username("disableduser")
                .password("$2a$10$encodedPassword")
                .email("disabled@example.com")
                .role(com.workflow.common.constant.Role.WORKER)
                .enabled(false)
                .build();

        LoginRequest disabledLoginRequest = new LoginRequest("disableduser", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.DisabledException("User is disabled"));

        // When/Then
        assertThrows(org.springframework.security.authentication.DisabledException.class, () -> {
            authenticationService.authenticate(disabledLoginRequest, httpRequest);
        });

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateToken(any());
    }

    // ============= Token Generation Tests =============

    @Test
    void shouldGenerateJwtTokenForUserDetails() {
        // Given
        when(jwtService.generateToken(testUser)).thenReturn("new-jwt-token");
        when(refreshTokenService.createRefreshToken(eq(testUser), eq(httpRequest))).thenReturn(mockRefreshToken);

        // When
        AuthenticationResponse response = authenticationService.generateJwtToken(testUser, httpRequest);

        // Then
        assertNotNull(response);
        assertEquals("new-jwt-token", response.getAccessToken());
        assertEquals("mock-refresh-token-uuid", response.getRefreshToken());
        verify(jwtService).generateToken(testUser);
        verify(refreshTokenService).createRefreshToken(eq(testUser), eq(httpRequest));
    }

    @Test
    void shouldReturnAuthenticationResponseWithToken() {
        // Given
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("sample-token");
        when(refreshTokenService.createRefreshToken(eq(testUser), eq(httpRequest))).thenReturn(mockRefreshToken);

        // When
        AuthenticationResponse response = authenticationService.generateJwtToken(testUser, httpRequest);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertEquals("sample-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
    }

    @Test
    void shouldGenerateTokenForDifferentUsers() {
        // Given
        User user1 = User.builder()
                .username("user1")
                .password("pass1")
                .role(com.workflow.common.constant.Role.WORKER)
                .enabled(true)
                .build();

        User user2 = User.builder()
                .username("user2")
                .password("pass2")
                .role(com.workflow.common.constant.Role.COMPANY)
                .enabled(true)
                .build();

        RefreshToken refreshToken1 = RefreshToken.builder()
                .token("refresh-token-1")
                .user(user1)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        RefreshToken refreshToken2 = RefreshToken.builder()
                .token("refresh-token-2")
                .user(user2)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        when(jwtService.generateToken(user1)).thenReturn("token-for-user1");
        when(jwtService.generateToken(user2)).thenReturn("token-for-user2");
        when(refreshTokenService.createRefreshToken(eq(user1), eq(httpRequest))).thenReturn(refreshToken1);
        when(refreshTokenService.createRefreshToken(eq(user2), eq(httpRequest))).thenReturn(refreshToken2);

        // When
        AuthenticationResponse response1 = authenticationService.generateJwtToken(user1, httpRequest);
        AuthenticationResponse response2 = authenticationService.generateJwtToken(user2, httpRequest);

        // Then
        assertEquals("token-for-user1", response1.getAccessToken());
        assertEquals("token-for-user2", response2.getAccessToken());
        assertNotEquals(response1.getAccessToken(), response2.getAccessToken());
    }

    // ============= Edge Cases =============

    @Test
    void shouldHandleNullUsernameInLoginRequest() {
        // Given
        LoginRequest nullUsernameRequest = new LoginRequest(null, "password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Username cannot be null"));

        // When/Then
        assertThrows(BadCredentialsException.class, () -> {
            authenticationService.authenticate(nullUsernameRequest, httpRequest);
        });
    }

    @Test
    void shouldHandleNullPasswordInLoginRequest() {
        // Given
        LoginRequest nullPasswordRequest = new LoginRequest("testuser", null);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Password cannot be null"));

        // When/Then
        assertThrows(BadCredentialsException.class, () -> {
            authenticationService.authenticate(nullPasswordRequest, httpRequest);
        });
    }

    @Test
    void shouldCallJwtServiceExactlyOncePerAuthentication() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("token");
        when(refreshTokenService.createRefreshToken(eq(testUser), eq(httpRequest))).thenReturn(mockRefreshToken);

        // When
        authenticationService.authenticate(loginRequest, httpRequest);

        // Then
        verify(jwtService, times(1)).generateToken(testUser);
    }
}