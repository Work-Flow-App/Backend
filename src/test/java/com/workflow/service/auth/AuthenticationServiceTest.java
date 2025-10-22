package com.workflow.service.auth;

import com.workflow.dto.auth.AuthenticationResponse;
import com.workflow.dto.auth.LoginRequest;
import com.workflow.entity.User;
import com.workflow.repository.UserRepository;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
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
    }

    // ============= Authentication Tests =============

    @Test
    void shouldAuthenticateUserWithValidCredentials() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // AuthenticationManager returns null on success
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("mock-jwt-token");

        // When
        AuthenticationResponse response = authenticationService.authenticate(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
        verify(jwtService).generateToken(testUser);
    }

    @Test
    void shouldReturnJwtTokenOnSuccessfulAuthentication() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("generated-token-abc123");

        // When
        AuthenticationResponse response = authenticationService.authenticate(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("generated-token-abc123", response.getToken());
        assertTrue(response.getToken().length() > 0);
    }

    @Test
    void shouldThrowExceptionForInvalidUsername() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When/Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            authenticationService.authenticate(loginRequest);
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
            authenticationService.authenticate(loginRequest);
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

        // When
        authenticationService.authenticate(loginRequest);

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
            authenticationService.authenticate(disabledLoginRequest);
        });

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateToken(any());
    }

    // ============= Token Generation Tests =============

    @Test
    void shouldGenerateJwtTokenForUserDetails() {
        // Given
        when(jwtService.generateToken(testUser)).thenReturn("new-jwt-token");

        // When
        AuthenticationResponse response = authenticationService.generateJwtToken(testUser);

        // Then
        assertNotNull(response);
        assertEquals("new-jwt-token", response.getToken());
        verify(jwtService).generateToken(testUser);
    }

    @Test
    void shouldReturnAuthenticationResponseWithToken() {
        // Given
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("sample-token");

        // When
        AuthenticationResponse response = authenticationService.generateJwtToken(testUser);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("sample-token", response.getToken());
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

        when(jwtService.generateToken(user1)).thenReturn("token-for-user1");
        when(jwtService.generateToken(user2)).thenReturn("token-for-user2");

        // When
        AuthenticationResponse response1 = authenticationService.generateJwtToken(user1);
        AuthenticationResponse response2 = authenticationService.generateJwtToken(user2);

        // Then
        assertEquals("token-for-user1", response1.getToken());
        assertEquals("token-for-user2", response2.getToken());
        assertNotEquals(response1.getToken(), response2.getToken());
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
            authenticationService.authenticate(nullUsernameRequest);
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
            authenticationService.authenticate(nullPasswordRequest);
        });
    }

    @Test
    void shouldCallJwtServiceExactlyOncePerAuthentication() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("token");

        // When
        authenticationService.authenticate(loginRequest);

        // Then
        verify(jwtService, times(1)).generateToken(testUser);
    }
}