package com.workflow.controller.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.dto.worker.WorkerCreateRequest;
import com.workflow.dto.worker.WorkerUpdateRequest;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.entity.Worker;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.UserRepository;
import com.workflow.repository.WorkerRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User companyUser;
    private Company company;
    private String companyUserToken;
    private Worker existingWorker;
    private User existingWorkerUser;

    @BeforeEach
    void setUp() {
        // Clear database
        workerRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        // Create company user
        companyUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("companyowner")
                .password(passwordEncoder.encode("password123"))
                .email("company@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();
        companyUser = userRepository.save(companyUser);

        // Create company
        company = Company.builder()
                .name("Test Company")
                .user(companyUser)
                .email("company@example.com")
                .archived(false)
                .build();
        company = companyRepository.save(company);

        // Create existing worker
        existingWorkerUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("existingworker")
                .password(passwordEncoder.encode("password123"))
                .email("existing.worker@example.com")
                .role(Role.WORKER)
                .enabled(true)
                .build();
        existingWorkerUser = userRepository.save(existingWorkerUser);

        existingWorker = Worker.builder()
                .name("Existing Worker")
                .initials("EW")
                .email("existing.worker@example.com")
                .telephone("1234567890")
                .mobile("0987654321")
                .company(company)
                .user(existingWorkerUser)
                .archived(false)
                .build();
        existingWorker = workerRepository.save(existingWorker);

        // Generate JWT token
        companyUserToken = jwtService.generateToken(companyUser);
    }

    // ============= POST /api/v1/workers Tests =============

    @Test
    void shouldCreateWorkerSuccessfully() throws Exception {
        WorkerCreateRequest request = new WorkerCreateRequest(
                "New Worker",
                "NW",
                "9999999999",
                "8888888888",
                "new.worker@example.com",
                "newworker",
                "password123"
        );

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Worker"))
                .andExpect(jsonPath("$.initials").value("NW"))
                .andExpect(jsonPath("$.telephone").value("9999999999"))
                .andExpect(jsonPath("$.mobile").value("8888888888"))
                .andExpect(jsonPath("$.email").value("new.worker@example.com"))
                .andExpect(jsonPath("$.username").value("newworker"))
                .andExpect(jsonPath("$.loginLocked").value(false))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        // Verify user account was created
        assert userRepository.findByUsername("newworker").isPresent();
        assert userRepository.findByUsername("newworker").get().getRole() == Role.WORKER;
    }

    @Test
    void shouldReturn403ForbiddenWithoutToken() throws Exception {
        WorkerCreateRequest request = new WorkerCreateRequest(
                "Worker", "W", null, null, null, "worker1", "password123"
        );

        mockMvc.perform(post("/api/v1/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400BadRequestForMissingRequiredFields() throws Exception {
        WorkerCreateRequest request = new WorkerCreateRequest(
                "",  // Empty name
                null,
                null,
                null,
                null,
                "",  // Empty username
                ""   // Empty password
        );

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void shouldReturn409ConflictForDuplicateUsername() throws Exception {
        WorkerCreateRequest request = new WorkerCreateRequest(
                "Another Worker",
                "AW",
                null,
                null,
                "another@example.com",
                "existingworker",  // Duplicate username
                "password123"
        );

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Username")))
                .andExpect(jsonPath("$.message").value(containsString("already taken")));
    }

    @Test
    void shouldReturn409ConflictForDuplicateEmail() throws Exception {
        WorkerCreateRequest request = new WorkerCreateRequest(
                "Another Worker",
                "AW",
                null,
                null,
                "existing.worker@example.com",  // Duplicate email
                "uniqueusername",
                "password123"
        );

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("email")))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void shouldValidateEmailFormat() throws Exception {
        WorkerCreateRequest request = new WorkerCreateRequest(
                "Worker",
                "W",
                null,
                null,
                "invalid-email",  // Invalid email
                "worker1",
                "password123"
        );

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void shouldValidatePasswordLength() throws Exception {
        WorkerCreateRequest request = new WorkerCreateRequest(
                "Worker",
                "W",
                null,
                null,
                null,
                "worker1",
                "short"  // Too short
        );

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists())
                .andExpect(jsonPath("$.validationErrors.password").value(containsString("at least 8 characters")));
    }

    // ============= GET /api/v1/workers Tests =============

    @Test
    void shouldGetAllWorkersSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/workers")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Existing Worker"))
                .andExpect(jsonPath("$[0].initials").value("EW"))
                .andExpect(jsonPath("$[0].email").value("existing.worker@example.com"));
    }

    @Test
    void shouldReturnEmptyArrayWhenNoWorkers() throws Exception {
        // Remove existing worker
        workerRepository.deleteAll();

        mockMvc.perform(get("/api/v1/workers")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ============= GET /api/v1/workers/{id} Tests =============

    @Test
    void shouldGetWorkerByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/workers/" + existingWorker.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingWorker.getId()))
                .andExpect(jsonPath("$.name").value("Existing Worker"))
                .andExpect(jsonPath("$.email").value("existing.worker@example.com"))
                .andExpect(jsonPath("$.username").value("existingworker"));
    }

    @Test
    void shouldReturn404WhenWorkerNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/workers/99999")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Worker not found")));
    }

    // ============= PUT /api/v1/workers/{id} Tests =============

    @Test
    void shouldUpdateWorkerSuccessfully() throws Exception {
        WorkerUpdateRequest request = new WorkerUpdateRequest(
                "Updated Worker Name",
                "UW",
                "1111111111",
                "2222222222",
                "updated.worker@example.com"
        );

        mockMvc.perform(put("/api/v1/workers/" + existingWorker.getId())
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Worker Name"))
                .andExpect(jsonPath("$.initials").value("UW"))
                .andExpect(jsonPath("$.telephone").value("1111111111"))
                .andExpect(jsonPath("$.mobile").value("2222222222"))
                .andExpect(jsonPath("$.email").value("updated.worker@example.com"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentWorker() throws Exception {
        WorkerUpdateRequest request = new WorkerUpdateRequest(
                "Worker", "W", null, null, "valid@example.com"
        );

        mockMvc.perform(put("/api/v1/workers/99999")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400BadRequestForInvalidEmailOnUpdate() throws Exception {
        WorkerUpdateRequest request = new WorkerUpdateRequest(
                "Worker",
                "W",
                null,
                null,
                "invalid-email"
        );

        mockMvc.perform(put("/api/v1/workers/" + existingWorker.getId())
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    // ============= DELETE /api/v1/workers/{id} Tests =============

    @Test
    void shouldDeleteWorkerSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/workers/" + existingWorker.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNoContent());

        // Verify worker is archived (soft delete)
        Worker deletedWorker = workerRepository.findById(existingWorker.getId()).orElseThrow();
        assert deletedWorker.isArchived();
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentWorker() throws Exception {
        mockMvc.perform(delete("/api/v1/workers/99999")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNotFound());
    }

    // ============= Authorization Tests =============

    @Test
    void shouldReturn403ForWorkerRoleAccessingWorkerEndpoints() throws Exception {
        User workerRoleUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("workerrole")
                .password(passwordEncoder.encode("password123"))
                .email("workerrole@example.com")
                .role(Role.WORKER)
                .enabled(true)
                .build();
        workerRoleUser = userRepository.save(workerRoleUser);
        String workerToken = jwtService.generateToken(workerRoleUser);

        mockMvc.perform(get("/api/v1/workers")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    // ============= Edge Cases =============

    @Test
    void shouldHandleWorkerWithMinimalInformation() throws Exception {
        WorkerCreateRequest request = new WorkerCreateRequest(
                "Minimal Worker",
                null,  // No initials
                null,  // No telephone
                null,  // No mobile
                null,  // No email
                "minimalworker",
                "password123"
        );

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Minimal Worker"))
                .andExpect(jsonPath("$.initials").value(nullValue()))
                .andExpect(jsonPath("$.telephone").value(nullValue()))
                .andExpect(jsonPath("$.mobile").value(nullValue()))
                .andExpect(jsonPath("$.email").value(nullValue()));
    }

    @Test
    void shouldValidateFieldLengths() throws Exception {
        WorkerCreateRequest request = new WorkerCreateRequest(
                "a".repeat(150),  // Exceeds 100 characters
                "a".repeat(20),   // Exceeds 10 characters
                null,
                null,
                null,
                "worker",
                "password123"
        );

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }
}