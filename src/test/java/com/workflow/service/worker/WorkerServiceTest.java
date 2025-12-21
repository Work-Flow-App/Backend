package com.workflow.service.worker;

import com.workflow.common.constant.Role;
import com.workflow.common.exception.business.UserAlreadyExistsException;
import com.workflow.common.exception.business.WorkerAlreadyExistsException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.dto.worker.WorkerCreateRequest;
import com.workflow.dto.worker.WorkerInviteResponse;
import com.workflow.dto.worker.WorkerResponse;
import com.workflow.dto.worker.WorkerUpdateRequest;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.entity.Worker;
import com.workflow.repository.UserRepository;
import com.workflow.repository.WorkerRepository;
import com.workflow.service.company.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyService companyService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private WorkerService workerService;

    private User companyUser;
    private Company company;
    private User workerUser;
    private Worker worker;
    private WorkerCreateRequest createRequest;
    private WorkerUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        companyUser = User.builder()
                .id(1L)
                .uuid("company-uuid")
                .username("companyowner")
                .password("encodedPassword")
                .email("company@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();

        company = Company.builder()
                .id(1L)
                .name("Test Company")
                .user(companyUser)
                .email("company@example.com")
                .archived(false)
                .createdAt(LocalDateTime.now())
                .build();

        workerUser = User.builder()
                .id(2L)
                .uuid("worker-uuid")
                .username("workeruser")
                .password("encodedPassword")
                .email("worker@example.com")
                .role(Role.WORKER)
                .enabled(true)
                .build();

        worker = Worker.builder()
                .id(1L)
                .name("John Worker")
                .initials("JW")
                .email("worker@example.com")
                .telephone("1234567890")
                .mobile("0987654321")
                .company(company)
                .user(workerUser)
                .loginLocked(false)
                .archived(false)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = new WorkerCreateRequest(
                "New Worker",
                "NW",
                "9999999999",
                "8888888888",
                "newworker@example.com",
                "newworker",
                "password123"
        );

        updateRequest = new WorkerUpdateRequest(
                "Updated Worker",
                "UW",
                "1111111111",
                "2222222222",
                "updated@example.com"
        );
    }

    // ============= createWorker Tests =============

    @Test
    void createWorker_ShouldCreateWorkerSuccessfully() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(userRepository.findByUsername("newworker")).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse("newworker@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(workerUser);
        when(workerRepository.save(any(Worker.class))).thenReturn(worker);

        // Act
        WorkerResponse response = workerService.createWorker(createRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(companyService).findCompanyByUserId(1L);
        verify(userRepository).findByUsername("newworker");
        verify(workerRepository).existsByEmailIgnoreCaseAndArchivedFalse("newworker@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(workerRepository).save(any(Worker.class));
    }

    @Test
    void createWorker_ShouldThrowException_WhenUsernameAlreadyExists() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(userRepository.findByUsername("newworker")).thenReturn(Optional.of(workerUser));

        // Act & Assert
        assertThatThrownBy(() -> workerService.createWorker(createRequest, 1L))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already taken");

        verify(companyService).findCompanyByUserId(1L);
        verify(userRepository).findByUsername("newworker");
        verify(userRepository, never()).save(any());
        verify(workerRepository, never()).save(any());
    }

    @Test
    void createWorker_ShouldThrowException_WhenEmailAlreadyExists() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(userRepository.findByUsername("newworker")).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse("newworker@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> workerService.createWorker(createRequest, 1L))
                .isInstanceOf(WorkerAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(companyService).findCompanyByUserId(1L);
        verify(userRepository).findByUsername("newworker");
        verify(workerRepository).existsByEmailIgnoreCaseAndArchivedFalse("newworker@example.com");
        verify(userRepository, never()).save(any());
        verify(workerRepository, never()).save(any());
    }

    @Test
    void createWorker_ShouldCreateUserWithCorrectRole() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(userRepository.findByUsername("newworker")).thenReturn(Optional.empty());
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse("newworker@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getUsername()).isEqualTo("newworker");
            assertThat(savedUser.getRole()).isEqualTo(Role.WORKER);
            assertThat(savedUser.isEnabled()).isTrue();
            assertThat(savedUser.getEmail()).isEqualTo("newworker@example.com");
            return savedUser;
        });
        when(workerRepository.save(any(Worker.class))).thenReturn(worker);

        // Act
        workerService.createWorker(createRequest, 1L);

        // Assert
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createWorker_ShouldHandleNullEmail() {
        // Arrange
        WorkerCreateRequest requestWithoutEmail = new WorkerCreateRequest(
                "Worker", "W", null, null, null, "worker123", "password123"
        );
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(userRepository.findByUsername("worker123")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(workerUser);
        when(workerRepository.save(any(Worker.class))).thenReturn(worker);

        // Act
        WorkerResponse response = workerService.createWorker(requestWithoutEmail, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(workerRepository, never()).existsByEmailIgnoreCaseAndArchivedFalse(anyString());
    }

    @Test
    void createWorker_ShouldHandleBlankEmail() {
        // Arrange
        WorkerCreateRequest requestWithBlankEmail = new WorkerCreateRequest(
                "Worker", "W", null, null, "", "worker123", "password123"
        );
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(userRepository.findByUsername("worker123")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(workerUser);
        when(workerRepository.save(any(Worker.class))).thenReturn(worker);

        // Act
        WorkerResponse response = workerService.createWorker(requestWithBlankEmail, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(workerRepository, never()).existsByEmailIgnoreCaseAndArchivedFalse(anyString());
    }

    // ============= getAllWorkers Tests =============

    @Test
    void getAllWorkers_ShouldReturnAllWorkersForCompany() {
        // Arrange
        User workerUser2 = User.builder()
                .id(3L)
                .uuid("worker2-uuid")
                .username("janeworker")
                .password("encodedPassword")
                .email("jane@example.com")
                .role(Role.WORKER)
                .enabled(true)
                .build();

        Worker worker2 = Worker.builder()
                .id(2L)
                .name("Jane Worker")
                .company(company)
                .user(workerUser2)
                .archived(false)
                .build();

        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByCompanyIdAndNotArchived(1L))
                .thenReturn(Arrays.asList(worker, worker2));

        // Act
        List<WorkerResponse> responses = workerService.getAllWorkers(1L);

        // Assert
        assertThat(responses).hasSize(2);
        verify(companyService).findCompanyByUserId(1L);
        verify(workerRepository).findByCompanyIdAndNotArchived(1L);
    }

    @Test
    void getAllWorkers_ShouldReturnEmptyList_WhenNoWorkers() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByCompanyIdAndNotArchived(1L)).thenReturn(Arrays.asList());

        // Act
        List<WorkerResponse> responses = workerService.getAllWorkers(1L);

        // Assert
        assertThat(responses).isEmpty();
        verify(companyService).findCompanyByUserId(1L);
        verify(workerRepository).findByCompanyIdAndNotArchived(1L);
    }

    // ============= getWorkerById Tests =============

    @Test
    void getWorkerById_ShouldReturnWorker() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(1L, 1L))
                .thenReturn(Optional.of(worker));

        // Act
        WorkerResponse response = workerService.getWorkerById(1L, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("John Worker");
        verify(companyService).findCompanyByUserId(1L);
        verify(workerRepository).findByIdAndCompanyIdAndNotArchived(1L, 1L);
    }

    @Test
    void getWorkerById_ShouldThrowException_WhenWorkerNotFound() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(99L, 1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> workerService.getWorkerById(99L, 1L))
                .isInstanceOf(WorkerNotFoundException.class)
                .hasMessageContaining("Worker not found");

        verify(companyService).findCompanyByUserId(1L);
        verify(workerRepository).findByIdAndCompanyIdAndNotArchived(99L, 1L);
    }

    // ============= updateWorker Tests =============

    @Test
    void updateWorker_ShouldUpdateWorkerSuccessfully() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(1L, 1L))
                .thenReturn(Optional.of(worker));
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse("updated@example.com"))
                .thenReturn(false);
        when(workerRepository.save(any(Worker.class))).thenReturn(worker);

        // Act
        WorkerResponse response = workerService.updateWorker(1L, updateRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(companyService).findCompanyByUserId(1L);
        verify(workerRepository).findByIdAndCompanyIdAndNotArchived(1L, 1L);
        verify(workerRepository).existsByEmailIgnoreCaseAndArchivedFalse("updated@example.com");
        verify(workerRepository).save(worker);
    }

    @Test
    void updateWorker_ShouldThrowException_WhenWorkerNotFound() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(99L, 1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> workerService.updateWorker(99L, updateRequest, 1L))
                .isInstanceOf(WorkerNotFoundException.class)
                .hasMessageContaining("Worker not found");

        verify(companyService).findCompanyByUserId(1L);
        verify(workerRepository).findByIdAndCompanyIdAndNotArchived(99L, 1L);
        verify(workerRepository, never()).save(any());
    }

    @Test
    void updateWorker_ShouldThrowException_WhenNewEmailAlreadyExists() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(1L, 1L))
                .thenReturn(Optional.of(worker));
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse("updated@example.com"))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> workerService.updateWorker(1L, updateRequest, 1L))
                .isInstanceOf(WorkerAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(companyService).findCompanyByUserId(1L);
        verify(workerRepository).findByIdAndCompanyIdAndNotArchived(1L, 1L);
        verify(workerRepository).existsByEmailIgnoreCaseAndArchivedFalse("updated@example.com");
        verify(workerRepository, never()).save(any());
    }

    @Test
    void updateWorker_ShouldAllowSameEmail() {
        // Arrange
        WorkerUpdateRequest sameEmailRequest = new WorkerUpdateRequest(
                "Updated Name", "UN", null, null, "worker@example.com" // Same email
        );
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(1L, 1L))
                .thenReturn(Optional.of(worker));
        when(workerRepository.save(any(Worker.class))).thenReturn(worker);

        // Act
        WorkerResponse response = workerService.updateWorker(1L, sameEmailRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(workerRepository, never()).existsByEmailIgnoreCaseAndArchivedFalse(anyString());
        verify(workerRepository).save(worker);
    }

    @Test
    void updateWorker_ShouldUpdateAllFields() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(1L, 1L))
                .thenReturn(Optional.of(worker));
        when(workerRepository.existsByEmailIgnoreCaseAndArchivedFalse("updated@example.com"))
                .thenReturn(false);
        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> {
            Worker savedWorker = invocation.getArgument(0);
            assertThat(savedWorker.getName()).isEqualTo("Updated Worker");
            assertThat(savedWorker.getInitials()).isEqualTo("UW");
            assertThat(savedWorker.getTelephone()).isEqualTo("1111111111");
            assertThat(savedWorker.getMobile()).isEqualTo("2222222222");
            assertThat(savedWorker.getEmail()).isEqualTo("updated@example.com");
            return savedWorker;
        });

        // Act
        workerService.updateWorker(1L, updateRequest, 1L);

        // Assert
        verify(workerRepository).save(worker);
    }

    @Test
    void updateWorker_ShouldHandleNullEmail() {
        // Arrange
        WorkerUpdateRequest requestWithNullEmail = new WorkerUpdateRequest(
                "Worker", "W", null, null, null
        );
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(1L, 1L))
                .thenReturn(Optional.of(worker));
        when(workerRepository.save(any(Worker.class))).thenReturn(worker);

        // Act
        WorkerResponse response = workerService.updateWorker(1L, requestWithNullEmail, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(workerRepository, never()).existsByEmailIgnoreCaseAndArchivedFalse(anyString());
    }

    // ============= deleteWorker Tests =============

    @Test
    void deleteWorker_ShouldArchiveWorkerSuccessfully() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(1L, 1L))
                .thenReturn(Optional.of(worker));
        when(workerRepository.save(any(Worker.class))).thenReturn(worker);

        // Act
        workerService.deleteWorker(1L, 1L);

        // Assert
        assertThat(worker.isArchived()).isTrue();
        verify(companyService).findCompanyByUserId(1L);
        verify(workerRepository).findByIdAndCompanyIdAndNotArchived(1L, 1L);
        verify(workerRepository).save(worker);
    }

    @Test
    void deleteWorker_ShouldThrowException_WhenWorkerNotFound() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(99L, 1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> workerService.deleteWorker(99L, 1L))
                .isInstanceOf(WorkerNotFoundException.class)
                .hasMessageContaining("Worker not found");

        verify(companyService).findCompanyByUserId(1L);
        verify(workerRepository).findByIdAndCompanyIdAndNotArchived(99L, 1L);
        verify(workerRepository, never()).save(any());
    }

    // ============= sendInvitation Tests =============

    @Test
    void sendInvitation_ShouldReturnInvitationResponse() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(1L, 1L))
                .thenReturn(Optional.of(worker));

        // Act
        WorkerInviteResponse response = workerService.sendInvitation(1L, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.workerId()).isEqualTo(1L);
        assertThat(response.workerName()).isEqualTo("John Worker");
        assertThat(response.email()).isEqualTo("worker@example.com");
        assertThat(response.message()).contains("Invitation email");
        verify(companyService).findCompanyByUserId(1L);
        verify(workerRepository).findByIdAndCompanyIdAndNotArchived(1L, 1L);
    }

    @Test
    void sendInvitation_ShouldThrowException_WhenWorkerNotFound() {
        // Arrange
        when(companyService.findCompanyByUserId(1L)).thenReturn(company);
        when(workerRepository.findByIdAndCompanyIdAndNotArchived(99L, 1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> workerService.sendInvitation(99L, 1L))
                .isInstanceOf(WorkerNotFoundException.class)
                .hasMessageContaining("Worker not found");

        verify(companyService).findCompanyByUserId(1L);
        verify(workerRepository).findByIdAndCompanyIdAndNotArchived(99L, 1L);
    }
}