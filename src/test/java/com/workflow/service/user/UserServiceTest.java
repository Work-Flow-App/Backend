package com.workflow.service.user;

import com.workflow.common.constant.Role;
import com.workflow.common.exception.business.UserAlreadyExistsException;
import com.workflow.dto.auth.SignupRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.auth.User;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.auth.UserRepository;
import com.workflow.service.jobtemplate.DefaultTemplateSeederService;
import com.workflow.service.subscription.ISubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DefaultTemplateSeederService defaultTemplateSeederService;

    @Mock
    private ISubscriptionService subscriptionService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private SignupRequest signupRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .uuid("test-uuid-123")
                .username("testuser")
                .password("$2a$10$encodedPassword")
                .email("test@example.com")
                .role(Role.WORKER)
                .enabled(true)
                .build();

        signupRequest = new SignupRequest(
                "newuser",
                "newuser@example.com",
                "password123",
                Role.WORKER
        );
    }

    // ============= Load User by Username Tests =============

    @Test
    void shouldLoadUserByUsernameSuccessfully() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.loadUserByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("$2a$10$encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void shouldThrowUsernameNotFoundExceptionIfUserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When/Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("nonexistent");
        });

        assertEquals("User not found with username : nonexistent", exception.getMessage());
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void shouldReturnUserDetailsWithCorrectAuthorities() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.loadUserByUsername("testuser");

        // Then
        assertNotNull(result.getAuthorities());
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_WORKER")));
    }

    // ============= Create User Tests =============

    @Test
    void shouldCreateUserWithValidData() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // When
        User result = userService.createUser(signupRequest);

        // Then
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("$2a$10$encodedNewPassword", result.getPassword());
        assertEquals("newuser@example.com", result.getEmail());
        assertEquals(Role.WORKER, result.getRole());
        assertFalse(result.isEnabled()); // disabled until email verification
        verify(userRepository).findByUsername("newuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldGenerateUuidForNewUser() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.createUser(signupRequest);

        // Then
        assertNotNull(result.getUuid());
        assertFalse(result.getUuid().isEmpty());
        // UUID format check (basic validation)
        assertTrue(result.getUuid().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void shouldEncodePasswordBeforeSaving() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.createUser(signupRequest);

        // Then
        assertEquals("$2a$10$encodedPassword123", result.getPassword());
        assertNotEquals("password123", result.getPassword()); // Should not save plain password
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void shouldSetEnabledFlagToFalseByDefault() {
        // Given — new users require email verification before being enabled
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.createUser(signupRequest);

        // Then
        assertFalse(result.isEnabled());
    }

    @Test
    void shouldThrowUserAlreadyExistsExceptionIfUsernameExists() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(testUser));

        // When/Then
        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            userService.createUser(signupRequest);
        });

        assertEquals("User already exists with username: newuser", exception.getMessage());
        verify(userRepository).findByUsername("newuser");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldAssignCorrectRoleToUser() {
        // Given - Test with different roles
        SignupRequest adminRequest = new SignupRequest("admin", "admin@test.com", "pass", Role.ADMIN);
        SignupRequest companyRequest = new SignupRequest("company", "company@test.com", "pass", Role.COMPANY);
        SignupRequest workerRequest = new SignupRequest("worker", "worker@test.com", "pass", Role.WORKER);

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User adminUser = userService.createUser(adminRequest);
        User companyUser = userService.createUser(companyRequest);
        User workerUser = userService.createUser(workerRequest);

        // Then
        assertEquals(Role.ADMIN, adminUser.getRole());
        assertEquals(Role.COMPANY, companyUser.getRole());
        assertEquals(Role.WORKER, workerUser.getRole());
    }

    @Test
    void shouldSaveUserToRepository() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.createUser(signupRequest);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("newuser@example.com", savedUser.getEmail());
        assertEquals(Role.WORKER, savedUser.getRole());
    }

    @Test
    void shouldCreateMultipleUsersSuccessfully() {
        // Given
        SignupRequest request1 = new SignupRequest("user1", "user1@test.com", "pass1", Role.WORKER);
        SignupRequest request2 = new SignupRequest("user2", "user2@test.com", "pass2", Role.COMPANY);

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User user1 = userService.createUser(request1);
        User user2 = userService.createUser(request2);

        // Then
        assertNotNull(user1);
        assertNotNull(user2);
        assertNotEquals(user1.getUuid(), user2.getUuid()); // Should have different UUIDs
        verify(userRepository, times(2)).save(any(User.class));
    }

    // ============= Edge Cases =============

    @Test
    void shouldHandleUsernameWithSpecialCharacters() {
        // Given
        SignupRequest specialRequest = new SignupRequest(
                "user.name-123",
                "user@example.com",
                "password",
                Role.WORKER
        );

        when(userRepository.findByUsername("user.name-123")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.createUser(specialRequest);

        // Then
        assertEquals("user.name-123", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldHandleEmailWithValidFormat() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.createUser(signupRequest);

        // Then
        assertEquals("newuser@example.com", result.getEmail());
    }

    @Test
    void shouldNotEncodePasswordTwice() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.createUser(signupRequest);

        // Then
        verify(passwordEncoder, times(1)).encode("password123");
    }

    // ============= Account Management Tests (Placeholder) =============
    // These methods are currently empty in the implementation

    @Test
    void deactivateAccountShouldBeImplemented() {
        // When
        userService.deactivateAccount("test-uuid");

        // Then - Currently does nothing, but test exists for future implementation
        // No assertions yet as method is not implemented
    }

    @Test
    void reactivateAccountShouldBeImplemented() {
        // When
        userService.reactivateAccount("test-uuid");

        // Then - Currently does nothing, but test exists for future implementation
        // No assertions yet as method is not implemented
    }

    @Test
    void deleteAccountShouldBeImplemented() {
        // When
        userService.deleteAccount("test-uuid");

        // Then - Currently does nothing, but test exists for future implementation
        // No assertions yet as method is not implemented
    }
}