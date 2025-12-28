package com.workflow.service.worker;

import com.workflow.common.constant.Role;
import com.workflow.common.exception.business.*;
import com.workflow.dto.worker.*;
import com.workflow.entity.*;
import com.workflow.repository.*;
import com.workflow.service.company.CompanyService;
import com.workflow.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("WorkerInvitationService Tests")
class WorkerInvitationServiceTest {

    @Mock
    private WorkerInvitationRepository invitationRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyService companyService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private WorkerInvitationService workerInvitationService;

    private Company testCompany;
    private User companyUser;
    private User workerUser;
    private Worker testWorker;
    private WorkerInvitation validInvitation;
    private WorkerInvitation expiredInvitation;
    private WorkerInvitation usedInvitation;

    private static final int EXPIRATION_DAYS = 7;
    private static final String TEST_EMAIL = "worker@example.com";
    private static final String TEST_TOKEN = "a3f2b9c1-4d5e-6f7g-8h9i-0j1k2l3m4n5o";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workerInvitationService, "expirationDays", EXPIRATION_DAYS);

        // Setup company
        companyUser = User.builder()
                .id(1L)
                .uuid("company-uuid-123")
                .username("companyuser")
                .email("company@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();

        testCompany = Company.builder()
                .id(1L)
                .name("Acme Corp")
                .user(companyUser)
                .archived(false)
                .build();

        // Setup worker user
        workerUser = User.builder()
                .id(2L)
                .uuid("worker-uuid-456")
                .username("workeruser")
                .email(TEST_EMAIL)
                .role(Role.WORKER)
                .enabled(true)
                .build();

        testWorker = Worker.builder()
                .id(1L)
                .name("John Doe")
                .email(TEST_EMAIL)
                .company(testCompany)
                .user(workerUser)
                .archived(false)
                .build();

        // Setup invitations
        validInvitation = WorkerInvitation.builder()
                .id(1L)
                .invitationToken(TEST_TOKEN)
                .email(TEST_EMAIL)
                .company(testCompany)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .used(false)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        expiredInvitation = WorkerInvitation.builder()
                .id(2L)
                .invitationToken("expired-token-123")
                .email("expired@example.com")
                .company(testCompany)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .used(false)
                .createdAt(LocalDateTime.now().minusDays(8))
                .build();

        usedInvitation = WorkerInvitation.builder()
                .id(3L)
                .invitationToken("used-token-456")
                .email("used@example.com")
                .company(testCompany)
                .expiresAt(LocalDateTime.now().plusDays(3))
                .used(true)
                .usedAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(4))
                .build();
    }

    // ===== CREATE INVITATION TESTS =====

    @Test
    @DisplayName("Should create invitation successfully")
    void createInvitation_Success() {
        // Arrange
        when(companyService.findCompanyByUserId(companyUser.getId())).thenReturn(testCompany);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(TEST_EMAIL)).thenReturn(false);
        when(invitationRepository.save(any(WorkerInvitation.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(emailService).sendWorkerInvitationEmail(anyString(), anyString(), anyString());

        // Act
        WorkerInviteResponse response = workerInvitationService.createInvitation(TEST_EMAIL, companyUser.getId());

        // Assert
        assertNotNull(response);
        assertEquals(TEST_EMAIL, response.email());
        assertEquals("Invitation sent successfully", response.message());
        assertNotNull(response.expiresAt());

        verify(invitationRepository).invalidatePreviousInvitations(TEST_EMAIL, testCompany.getId());
        verify(invitationRepository).save(any(WorkerInvitation.class));
        verify(emailService).sendWorkerInvitationEmail(eq(TEST_EMAIL), eq(testCompany.getName()), anyString());
    }

    @Test
    @DisplayName("Should invalidate previous invitations when creating new one")
    void createInvitation_InvalidatesPreviousInvitations() {
        // Arrange
        when(companyService.findCompanyByUserId(companyUser.getId())).thenReturn(testCompany);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(TEST_EMAIL)).thenReturn(false);
        when(invitationRepository.save(any(WorkerInvitation.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        workerInvitationService.createInvitation(TEST_EMAIL, companyUser.getId());

        // Assert
        verify(invitationRepository).invalidatePreviousInvitations(TEST_EMAIL, testCompany.getId());
    }

    @Test
    @DisplayName("Should throw exception when email already registered")
    void createInvitation_EmailAlreadyRegistered() {
        // Arrange
        when(companyService.findCompanyByUserId(companyUser.getId())).thenReturn(testCompany);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(workerUser));

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> workerInvitationService.createInvitation(TEST_EMAIL, companyUser.getId())
        );

        assertEquals("Email already registered", exception.getMessage());
        verify(invitationRepository, never()).save(any());
        verify(emailService, never()).sendWorkerInvitationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when worker already exists")
    void createInvitation_WorkerAlreadyExists() {
        // Arrange
        when(companyService.findCompanyByUserId(companyUser.getId())).thenReturn(testCompany);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(TEST_EMAIL)).thenReturn(true);

        // Act & Assert
        WorkerAlreadyExistsException exception = assertThrows(
                WorkerAlreadyExistsException.class,
                () -> workerInvitationService.createInvitation(TEST_EMAIL, companyUser.getId())
        );

        assertEquals("Worker with this email already exists", exception.getMessage());
        verify(invitationRepository, never()).save(any());
        verify(emailService, never()).sendWorkerInvitationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should generate UUID token when creating invitation")
    void createInvitation_GeneratesUuidToken() {
        // Arrange
        when(companyService.findCompanyByUserId(companyUser.getId())).thenReturn(testCompany);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(TEST_EMAIL)).thenReturn(false);
        when(invitationRepository.save(any(WorkerInvitation.class))).thenAnswer(i -> i.getArguments()[0]);

        ArgumentCaptor<WorkerInvitation> invitationCaptor = ArgumentCaptor.forClass(WorkerInvitation.class);

        // Act
        workerInvitationService.createInvitation(TEST_EMAIL, companyUser.getId());

        // Assert
        verify(invitationRepository).save(invitationCaptor.capture());
        WorkerInvitation savedInvitation = invitationCaptor.getValue();

        assertNotNull(savedInvitation.getInvitationToken());
        assertTrue(savedInvitation.getInvitationToken().length() > 30); // UUID is longer than 30 chars
        assertEquals(TEST_EMAIL, savedInvitation.getEmail());
        assertEquals(testCompany, savedInvitation.getCompany());
        assertFalse(savedInvitation.isUsed());
    }

    @Test
    @DisplayName("Should set expiration to 7 days from now")
    void createInvitation_SetsCorrectExpiration() {
        // Arrange
        when(companyService.findCompanyByUserId(companyUser.getId())).thenReturn(testCompany);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(TEST_EMAIL)).thenReturn(false);
        when(invitationRepository.save(any(WorkerInvitation.class))).thenAnswer(i -> i.getArguments()[0]);

        ArgumentCaptor<WorkerInvitation> invitationCaptor = ArgumentCaptor.forClass(WorkerInvitation.class);

        // Act
        workerInvitationService.createInvitation(TEST_EMAIL, companyUser.getId());

        // Assert
        verify(invitationRepository).save(invitationCaptor.capture());
        WorkerInvitation savedInvitation = invitationCaptor.getValue();

        LocalDateTime expectedExpiration = LocalDateTime.now().plusDays(EXPIRATION_DAYS);
        assertTrue(savedInvitation.getExpiresAt().isAfter(LocalDateTime.now()));
        assertTrue(savedInvitation.getExpiresAt().isBefore(expectedExpiration.plusMinutes(1)));
    }

    // ===== VALIDATE AND ACCEPT INVITATION TESTS =====

    @Test
    @DisplayName("Should accept invitation and create worker successfully")
    void validateAndAcceptInvitation_Success() {
        // Arrange
        WorkerSignupRequest request = new WorkerSignupRequest(
                TEST_TOKEN,
                TEST_EMAIL,
                "John Doe",
                "JD",
                "123-456-7890",
                "987-654-3210",
                "johndoe",
                "password123"
        );

        when(invitationRepository.findByInvitationToken(TEST_TOKEN)).thenReturn(Optional.of(validInvitation));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> {
            Worker w = (Worker) i.getArguments()[0];
            w.setId(1L);
            return w;
        });
        when(invitationRepository.save(any(WorkerInvitation.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        WorkerSignupResponse response = workerInvitationService.validateAndAcceptInvitation(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.workerId());
        assertEquals("John Doe", response.name());
        assertEquals(TEST_EMAIL, response.email());
        assertEquals("johndoe", response.username());
        assertEquals(testCompany.getName(), response.companyName());
        assertEquals("Account created successfully", response.message());

        verify(userRepository).save(any(User.class));
        verify(workerRepository).save(any(Worker.class));
        verify(invitationRepository).save(argThat(inv -> inv.isUsed()));
    }

    @Test
    @DisplayName("Should throw exception when invitation token not found")
    void validateAndAcceptInvitation_TokenNotFound() {
        // Arrange
        WorkerSignupRequest request = new WorkerSignupRequest(
                "invalid-token",
                TEST_EMAIL,
                "John Doe",
                null,
                null,
                null,
                "johndoe",
                "password123"
        );

        when(invitationRepository.findByInvitationToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        InvalidWorkerInvitationException exception = assertThrows(
                InvalidWorkerInvitationException.class,
                () -> workerInvitationService.validateAndAcceptInvitation(request)
        );

        assertEquals("Invalid or expired invitation token", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when invitation is expired")
    void validateAndAcceptInvitation_InvitationExpired() {
        // Arrange
        WorkerSignupRequest request = new WorkerSignupRequest(
                expiredInvitation.getInvitationToken(),
                expiredInvitation.getEmail(),
                "John Doe",
                null,
                null,
                null,
                "johndoe",
                "password123"
        );

        when(invitationRepository.findByInvitationToken(expiredInvitation.getInvitationToken()))
                .thenReturn(Optional.of(expiredInvitation));

        // Act & Assert
        InvalidWorkerInvitationException exception = assertThrows(
                InvalidWorkerInvitationException.class,
                () -> workerInvitationService.validateAndAcceptInvitation(request)
        );

        assertEquals("Invalid or expired invitation token", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when invitation already used")
    void validateAndAcceptInvitation_InvitationAlreadyUsed() {
        // Arrange
        WorkerSignupRequest request = new WorkerSignupRequest(
                usedInvitation.getInvitationToken(),
                usedInvitation.getEmail(),
                "John Doe",
                null,
                null,
                null,
                "johndoe",
                "password123"
        );

        when(invitationRepository.findByInvitationToken(usedInvitation.getInvitationToken()))
                .thenReturn(Optional.of(usedInvitation));

        // Act & Assert
        InvalidWorkerInvitationException exception = assertThrows(
                InvalidWorkerInvitationException.class,
                () -> workerInvitationService.validateAndAcceptInvitation(request)
        );

        assertEquals("Invitation has already been used", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when email does not match invitation")
    void validateAndAcceptInvitation_EmailMismatch() {
        // Arrange
        WorkerSignupRequest request = new WorkerSignupRequest(
                TEST_TOKEN,
                "different@example.com",
                "John Doe",
                null,
                null,
                null,
                "johndoe",
                "password123"
        );

        when(invitationRepository.findByInvitationToken(TEST_TOKEN)).thenReturn(Optional.of(validInvitation));

        // Act & Assert
        InvalidWorkerInvitationException exception = assertThrows(
                InvalidWorkerInvitationException.class,
                () -> workerInvitationService.validateAndAcceptInvitation(request)
        );

        assertEquals("Email does not match invitation", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when username already taken")
    void validateAndAcceptInvitation_UsernameTaken() {
        // Arrange
        WorkerSignupRequest request = new WorkerSignupRequest(
                TEST_TOKEN,
                TEST_EMAIL,
                "John Doe",
                null,
                null,
                null,
                "existinguser",
                "password123"
        );

        User existingUser = User.builder().id(99L).username("existinguser").build();

        when(invitationRepository.findByInvitationToken(TEST_TOKEN)).thenReturn(Optional.of(validInvitation));
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> workerInvitationService.validateAndAcceptInvitation(request)
        );

        assertTrue(exception.getMessage().contains("existinguser"));
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when company is archived")
    void validateAndAcceptInvitation_CompanyArchived() {
        // Arrange
        testCompany.setArchived(true);

        WorkerSignupRequest request = new WorkerSignupRequest(
                TEST_TOKEN,
                TEST_EMAIL,
                "John Doe",
                null,
                null,
                null,
                "johndoe",
                "password123"
        );

        when(invitationRepository.findByInvitationToken(TEST_TOKEN)).thenReturn(Optional.of(validInvitation));

        // Act & Assert
        CompanyNotFoundException exception = assertThrows(
                CompanyNotFoundException.class,
                () -> workerInvitationService.validateAndAcceptInvitation(request)
        );

        assertEquals("Company not found", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark invitation as used before creating worker")
    void validateAndAcceptInvitation_MarksInvitationUsedFirst() {
        // Arrange
        WorkerSignupRequest request = new WorkerSignupRequest(
                TEST_TOKEN,
                TEST_EMAIL,
                "John Doe",
                null,
                null,
                null,
                "johndoe",
                "password123"
        );

        when(invitationRepository.findByInvitationToken(TEST_TOKEN)).thenReturn(Optional.of(validInvitation));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(invitationRepository.save(any(WorkerInvitation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> {
            Worker w = (Worker) i.getArguments()[0];
            w.setId(1L);
            return w;
        });

        // Act
        workerInvitationService.validateAndAcceptInvitation(request);

        // Assert - invitation should be saved as used
        ArgumentCaptor<WorkerInvitation> invitationCaptor = ArgumentCaptor.forClass(WorkerInvitation.class);
        verify(invitationRepository).save(invitationCaptor.capture());

        WorkerInvitation savedInvitation = invitationCaptor.getValue();
        assertTrue(savedInvitation.isUsed());
        assertNotNull(savedInvitation.getUsedAt());
    }

    @Test
    @DisplayName("Should create user with WORKER role and encoded password")
    void validateAndAcceptInvitation_CreatesUserCorrectly() {
        // Arrange
        WorkerSignupRequest request = new WorkerSignupRequest(
                TEST_TOKEN,
                TEST_EMAIL,
                "John Doe",
                null,
                null,
                null,
                "johndoe",
                "password123"
        );

        when(invitationRepository.findByInvitationToken(TEST_TOKEN)).thenReturn(Optional.of(validInvitation));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(invitationRepository.save(any(WorkerInvitation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> {
            Worker w = (Worker) i.getArguments()[0];
            w.setId(1L);
            return w;
        });

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // Act
        workerInvitationService.validateAndAcceptInvitation(request);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("johndoe", savedUser.getUsername());
        assertEquals(TEST_EMAIL, savedUser.getEmail());
        assertEquals("$2a$10$encodedPassword", savedUser.getPassword());
        assertEquals(Role.WORKER, savedUser.getRole());
        assertTrue(savedUser.isEnabled());
    }

    @Test
    @DisplayName("Should create worker with correct company association")
    void validateAndAcceptInvitation_CreatesWorkerCorrectly() {
        // Arrange
        WorkerSignupRequest request = new WorkerSignupRequest(
                TEST_TOKEN,
                TEST_EMAIL,
                "John Doe",
                "JD",
                "123-456-7890",
                "987-654-3210",
                "johndoe",
                "password123"
        );

        when(invitationRepository.findByInvitationToken(TEST_TOKEN)).thenReturn(Optional.of(validInvitation));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(invitationRepository.save(any(WorkerInvitation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> {
            Worker w = (Worker) i.getArguments()[0];
            w.setId(1L);
            return w;
        });

        ArgumentCaptor<Worker> workerCaptor = ArgumentCaptor.forClass(Worker.class);

        // Act
        workerInvitationService.validateAndAcceptInvitation(request);

        // Assert
        verify(workerRepository).save(workerCaptor.capture());
        Worker savedWorker = workerCaptor.getValue();

        assertEquals("John Doe", savedWorker.getName());
        assertEquals("JD", savedWorker.getInitials());
        assertEquals("123-456-7890", savedWorker.getTelephone());
        assertEquals("987-654-3210", savedWorker.getMobile());
        assertEquals(TEST_EMAIL, savedWorker.getEmail());
        assertEquals(testCompany, savedWorker.getCompany());
        assertNotNull(savedWorker.getUser());
        assertFalse(savedWorker.isLoginLocked());
        assertFalse(savedWorker.isArchived());
    }

    // ===== GET INVITATIONS BY COMPANY TESTS =====

    @Test
    @DisplayName("Should return all invitations for company")
    void getInvitationsByCompany_Success() {
        // Arrange
        List<WorkerInvitation> invitations = Arrays.asList(
                validInvitation,
                expiredInvitation,
                usedInvitation
        );

        when(companyService.findCompanyByUserId(companyUser.getId())).thenReturn(testCompany);
        when(invitationRepository.findByCompanyIdOrderByCreatedAtDesc(testCompany.getId()))
                .thenReturn(invitations);

        // Act
        List<WorkerInvitationStatusResponse> responses =
                workerInvitationService.getInvitationsByCompany(companyUser.getId());

        // Assert
        assertNotNull(responses);
        assertEquals(3, responses.size());

        verify(companyService).findCompanyByUserId(companyUser.getId());
        verify(invitationRepository).findByCompanyIdOrderByCreatedAtDesc(testCompany.getId());
    }

    @Test
    @DisplayName("Should return PENDING status for valid unexpired invitation")
    void getInvitationsByCompany_PendingStatus() {
        // Arrange
        when(companyService.findCompanyByUserId(companyUser.getId())).thenReturn(testCompany);
        when(invitationRepository.findByCompanyIdOrderByCreatedAtDesc(testCompany.getId()))
                .thenReturn(List.of(validInvitation));

        // Act
        List<WorkerInvitationStatusResponse> responses =
                workerInvitationService.getInvitationsByCompany(companyUser.getId());

        // Assert
        assertEquals(1, responses.size());
        WorkerInvitationStatusResponse response = responses.get(0);
        assertEquals("PENDING", response.status());
        assertEquals(validInvitation.getId(), response.invitationId());
        assertEquals(validInvitation.getEmail(), response.email());
        assertNull(response.usedAt());
    }

    @Test
    @DisplayName("Should return EXPIRED status for expired invitation")
    void getInvitationsByCompany_ExpiredStatus() {
        // Arrange
        when(companyService.findCompanyByUserId(companyUser.getId())).thenReturn(testCompany);
        when(invitationRepository.findByCompanyIdOrderByCreatedAtDesc(testCompany.getId()))
                .thenReturn(List.of(expiredInvitation));

        // Act
        List<WorkerInvitationStatusResponse> responses =
                workerInvitationService.getInvitationsByCompany(companyUser.getId());

        // Assert
        assertEquals(1, responses.size());
        WorkerInvitationStatusResponse response = responses.get(0);
        assertEquals("EXPIRED", response.status());
        assertEquals(expiredInvitation.getEmail(), response.email());
        assertNull(response.usedAt());
    }

    @Test
    @DisplayName("Should return ACCEPTED status for used invitation")
    void getInvitationsByCompany_AcceptedStatus() {
        // Arrange
        when(companyService.findCompanyByUserId(companyUser.getId())).thenReturn(testCompany);
        when(invitationRepository.findByCompanyIdOrderByCreatedAtDesc(testCompany.getId()))
                .thenReturn(List.of(usedInvitation));

        // Act
        List<WorkerInvitationStatusResponse> responses =
                workerInvitationService.getInvitationsByCompany(companyUser.getId());

        // Assert
        assertEquals(1, responses.size());
        WorkerInvitationStatusResponse response = responses.get(0);
        assertEquals("ACCEPTED", response.status());
        assertEquals(usedInvitation.getEmail(), response.email());
        assertNotNull(response.usedAt());
    }

    @Test
    @DisplayName("Should return empty list when no invitations exist")
    void getInvitationsByCompany_EmptyList() {
        // Arrange
        when(companyService.findCompanyByUserId(companyUser.getId())).thenReturn(testCompany);
        when(invitationRepository.findByCompanyIdOrderByCreatedAtDesc(testCompany.getId()))
                .thenReturn(List.of());

        // Act
        List<WorkerInvitationStatusResponse> responses =
                workerInvitationService.getInvitationsByCompany(companyUser.getId());

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    @DisplayName("Should handle email with different case in invitation validation")
    void validateAndAcceptInvitation_EmailCaseInsensitive() {
        // Arrange
        WorkerSignupRequest request = new WorkerSignupRequest(
                TEST_TOKEN,
                "WORKER@EXAMPLE.COM", // Different case
                "John Doe",
                null,
                null,
                null,
                "johndoe",
                "password123"
        );

        when(invitationRepository.findByInvitationToken(TEST_TOKEN)).thenReturn(Optional.of(validInvitation));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(invitationRepository.save(any(WorkerInvitation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> {
            Worker w = (Worker) i.getArguments()[0];
            w.setId(1L);
            return w;
        });

        // Act
        WorkerSignupResponse response = workerInvitationService.validateAndAcceptInvitation(request);

        // Assert
        assertNotNull(response);
        assertEquals("Account created successfully", response.message());
    }
}
