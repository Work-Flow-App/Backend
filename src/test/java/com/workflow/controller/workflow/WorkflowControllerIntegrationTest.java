package com.workflow.controller.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.dto.workflow.WorkflowBulkUpdateRequest;
import com.workflow.dto.workflow.WorkflowCreateRequest;
import com.workflow.dto.workflow.WorkflowStepBulkRequest;
import com.workflow.dto.workflow.WorkflowStepCreateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.workflow.Workflow;
import com.workflow.entity.workflow.WorkflowStep;
import com.workflow.entity.auth.User;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.workflow.WorkflowRepository;
import com.workflow.repository.workflow.WorkflowStepRepository;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkflowControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private WorkflowRepository workflowRepository;
    @Autowired private WorkflowStepRepository workflowStepRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Company company;
    private Company anotherCompany;
    private Workflow existingWorkflow;
    private WorkflowStep existingStep;
    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        workflowStepRepository.deleteAll();
        workflowRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        User companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("workflowowner")
                .password(passwordEncoder.encode("password")).email("workflowowner@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anotherworkflowowner")
                .password(passwordEncoder.encode("password")).email("anotherworkflow@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("workflowworker")
                .password(passwordEncoder.encode("password")).email("workflowworker@test.com")
                .role(Role.WORKER).enabled(true).build());

        company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("workflowowner@test.com").archived(false).build());

        anotherCompany = companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("anotherworkflow@test.com").archived(false).build());

        existingWorkflow = workflowRepository.save(Workflow.builder()
                .company(company)
                .name("Test Workflow")
                .description("A test workflow")
                .build());

        existingStep = workflowStepRepository.save(WorkflowStep.builder()
                .workflow(existingWorkflow)
                .name("Step One")
                .description("First step")
                .orderIndex(1)
                .optional(false)
                .build());

        companyToken = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken = jwtService.generateToken(workerUser);
    }

    // ============= POST /api/v1/workflows =============

    @Test
    void shouldCreateWorkflowSuccessfully() throws Exception {
        WorkflowCreateRequest request = WorkflowCreateRequest.builder()
                .name("New Workflow")
                .description("New workflow description")
                .build();

        mockMvc.perform(post("/api/v1/workflows")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Workflow"))
                .andExpect(jsonPath("$.description").value("New workflow description"))
                .andExpect(jsonPath("$.companyId").exists());
    }

    @Test
    void shouldReturn400WhenWorkflowNameIsBlank() throws Exception {
        WorkflowCreateRequest request = WorkflowCreateRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/api/v1/workflows")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403WhenCreatingWorkflowWithoutToken() throws Exception {
        WorkflowCreateRequest request = WorkflowCreateRequest.builder().name("Workflow").build();

        mockMvc.perform(post("/api/v1/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404ForWorkerRoleOnCreateWorkflowBecauseNoCompany() throws Exception {
        // /api/v1/workflows/** is not restricted to COMPANY role in SecurityConfig;
        // getCompanyId() throws CompanyNotFoundException (404) for worker users
        WorkflowCreateRequest request = WorkflowCreateRequest.builder().name("Workflow Name").build();

        mockMvc.perform(post("/api/v1/workflows")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= PUT /api/v1/workflows/{id} =============

    @Test
    void shouldUpdateWorkflowSuccessfully() throws Exception {
        WorkflowCreateRequest request = WorkflowCreateRequest.builder()
                .name("Updated Workflow")
                .description("Updated description")
                .build();

        mockMvc.perform(put("/api/v1/workflows/" + existingWorkflow.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingWorkflow.getId()))
                .andExpect(jsonPath("$.name").value("Updated Workflow"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentWorkflow() throws Exception {
        WorkflowCreateRequest request = WorkflowCreateRequest.builder().name("Workflow").build();

        mockMvc.perform(put("/api/v1/workflows/99999")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingAnotherCompanyWorkflow() throws Exception {
        WorkflowCreateRequest request = WorkflowCreateRequest.builder().name("Updated").build();

        mockMvc.perform(put("/api/v1/workflows/" + existingWorkflow.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= DELETE /api/v1/workflows/{id} =============

    @Test
    void shouldDeleteWorkflowSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/workflows/" + existingWorkflow.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentWorkflow() throws Exception {
        mockMvc.perform(delete("/api/v1/workflows/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingAnotherCompanyWorkflow() throws Exception {
        mockMvc.perform(delete("/api/v1/workflows/" + existingWorkflow.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/workflows =============

    @Test
    void shouldGetAllWorkflowsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/workflows")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(existingWorkflow.getId()))
                .andExpect(jsonPath("$[0].name").value("Test Workflow"));
    }

    @Test
    void shouldReturnEmptyListForAnotherCompany() throws Exception {
        mockMvc.perform(get("/api/v1/workflows")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ============= GET /api/v1/workflows/{id} =============

    @Test
    void shouldGetWorkflowByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/" + existingWorkflow.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingWorkflow.getId()))
                .andExpect(jsonPath("$.name").value("Test Workflow"))
                .andExpect(jsonPath("$.description").value("A test workflow"));
    }

    @Test
    void shouldReturn404WhenWorkflowNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAccessingAnotherCompanyWorkflow() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/" + existingWorkflow.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= POST /api/v1/workflows/steps =============

    @Test
    void shouldCreateStepSuccessfully() throws Exception {
        WorkflowStepCreateRequest request = WorkflowStepCreateRequest.builder()
                .workflowId(existingWorkflow.getId())
                .name("New Step")
                .description("Step description")
                .orderIndex(2)
                .optional(false)
                .build();

        mockMvc.perform(post("/api/v1/workflows/steps")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Step"))
                .andExpect(jsonPath("$.workflowId").value(existingWorkflow.getId()))
                .andExpect(jsonPath("$.orderIndex").value(2));
    }

    @Test
    void shouldReturn403WhenCreatingStepWithoutToken() throws Exception {
        // No auth header → Spring Security rejects with 403 before reaching the controller
        WorkflowStepCreateRequest request = WorkflowStepCreateRequest.builder()
                .workflowId(existingWorkflow.getId()).name("Step Name").orderIndex(1).build();

        mockMvc.perform(post("/api/v1/workflows/steps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/workflows/{workflowId}/steps =============

    @Test
    void shouldGetStepsForWorkflowSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/" + existingWorkflow.getId() + "/steps")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(existingStep.getId()))
                .andExpect(jsonPath("$[0].name").value("Step One"))
                .andExpect(jsonPath("$[0].orderIndex").value(1));
    }

    @Test
    void shouldReturn403WhenAccessingStepsOfAnotherCompanyWorkflow() throws Exception {
        // The service throws UnauthorizedWorkflowAccessException (extends ForbiddenException)
        // when a company tries to access a workflow that doesn't belong to them.
        mockMvc.perform(get("/api/v1/workflows/" + existingWorkflow.getId() + "/steps")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/workflows/steps/{stepId} =============

    @Test
    void shouldGetStepByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/steps/" + existingStep.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingStep.getId()))
                .andExpect(jsonPath("$.name").value("Step One"))
                .andExpect(jsonPath("$.orderIndex").value(1));
    }

    @Test
    void shouldReturn404WhenStepNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/steps/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAccessingAnotherCompanyStep() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/steps/" + existingStep.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/workflows/steps =============

    @Test
    void shouldGetAllStepsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/steps")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Step One"));
    }

    @Test
    void shouldReturnEmptyStepsForCompanyWithNoSteps() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/steps")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ============= PUT /api/v1/workflows/steps/{stepId} =============

    @Test
    void shouldUpdateStepSuccessfully() throws Exception {
        WorkflowStepCreateRequest request = WorkflowStepCreateRequest.builder()
                .workflowId(existingWorkflow.getId())
                .name("Updated Step")
                .description("Updated description")
                .orderIndex(1)
                .optional(true)
                .build();

        mockMvc.perform(put("/api/v1/workflows/steps/" + existingStep.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingStep.getId()))
                .andExpect(jsonPath("$.name").value("Updated Step"))
                .andExpect(jsonPath("$.optional").value(true));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentStep() throws Exception {
        WorkflowStepCreateRequest request = WorkflowStepCreateRequest.builder()
                .workflowId(existingWorkflow.getId()).name("Step").orderIndex(1).build();

        mockMvc.perform(put("/api/v1/workflows/steps/99999")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingAnotherCompanyStep() throws Exception {
        WorkflowStepCreateRequest request = WorkflowStepCreateRequest.builder()
                .workflowId(existingWorkflow.getId()).name("Updated").orderIndex(1).build();

        mockMvc.perform(put("/api/v1/workflows/steps/" + existingStep.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= DELETE /api/v1/workflows/steps/{stepId} =============

    @Test
    void shouldDeleteStepSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/workflows/steps/" + existingStep.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentStep() throws Exception {
        mockMvc.perform(delete("/api/v1/workflows/steps/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    // ============= PUT /api/v1/workflows/{workflowId}/bulk =============

    @Test
    void shouldBulkUpdateWorkflowSuccessfully() throws Exception {
        WorkflowStepBulkRequest newStep = WorkflowStepBulkRequest.builder()
                .name("Bulk New Step").description("Added via bulk").orderIndex(2).optional(false).build();

        WorkflowBulkUpdateRequest request = WorkflowBulkUpdateRequest.builder()
                .name("Bulk Updated Workflow")
                .description("Bulk updated")
                .steps(List.of(newStep))
                .build();

        mockMvc.perform(put("/api/v1/workflows/" + existingWorkflow.getId() + "/bulk")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingWorkflow.getId()))
                .andExpect(jsonPath("$.name").value("Bulk Updated Workflow"));
    }

    @Test
    void shouldReturn404WhenBulkUpdatingNonExistentWorkflow() throws Exception {
        WorkflowBulkUpdateRequest request = WorkflowBulkUpdateRequest.builder()
                .name("Workflow").steps(List.of()).build();

        mockMvc.perform(put("/api/v1/workflows/99999/bulk")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/workflows/{workflowId}/with-steps =============

    @Test
    void shouldGetWorkflowWithStepsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/" + existingWorkflow.getId() + "/with-steps")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingWorkflow.getId()))
                .andExpect(jsonPath("$.name").value("Test Workflow"))
                .andExpect(jsonPath("$.steps").isArray())
                .andExpect(jsonPath("$.steps", hasSize(1)))
                .andExpect(jsonPath("$.steps[0].name").value("Step One"));
    }

    @Test
    void shouldReturn404WhenGettingNonExistentWorkflowWithSteps() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/99999/with-steps")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401ForInvalidTokenOnWorkflows() throws Exception {
        mockMvc.perform(get("/api/v1/workflows")
                        .header("Authorization", "Bearer bad.token.here"))
                .andExpect(status().isUnauthorized());
    }
}
