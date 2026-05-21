package com.workflow.controller.workflow;

import com.workflow.AbstractControllerIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.dto.workflow.JobWorkflowStepCreateRequest;
import com.workflow.dto.workflow.JobWorkflowStepUpdateRequest;
import com.workflow.dto.workflow.JobWorkflowUpdateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.job.JobWorkflow;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.Worker;
import com.workflow.entity.workflow.Workflow;
import com.workflow.entity.workflow.WorkflowStep;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.customer.CustomerRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.job.JobTemplateRepository;
import com.workflow.repository.job.JobWorkflowRepository;
import com.workflow.repository.job.JobWorkflowStepActivityRepository;
import com.workflow.repository.job.JobWorkflowStepRepository;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.repository.workflow.WorkflowRepository;
import com.workflow.repository.workflow.WorkflowStepRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JobWorkflowControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobTemplateRepository jobTemplateRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private WorkflowRepository workflowRepository;
    @Autowired private WorkflowStepRepository workflowStepRepository;
    @Autowired private JobWorkflowRepository jobWorkflowRepository;
    @Autowired private JobWorkflowStepRepository jobWorkflowStepRepository;
    @Autowired private JobWorkflowStepActivityRepository jobWorkflowStepActivityRepository;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Company company;
    private Company anotherCompany;
    private Job job;
    private Job jobWithWorkflow;
    private Workflow workflow;
    private WorkflowStep workflowStep;
    private JobWorkflow existingJobWorkflow;
    private JobWorkflowStep existingJobWorkflowStep;
    private Worker worker;
    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        jobWorkflowStepActivityRepository.deleteAll();
        jobWorkflowStepRepository.deleteAll();
        jobWorkflowRepository.deleteAll();
        workerRepository.deleteAll();
        jobRepository.deleteAll();
        jobTemplateRepository.deleteAll();
        customerRepository.deleteAll();
        workflowStepRepository.deleteAll();
        workflowRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        User companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("jwowner")
                .password(passwordEncoder.encode("password")).email("jwowner@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anotherjwowner")
                .password(passwordEncoder.encode("password")).email("anotherjw@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("jwworker")
                .password(passwordEncoder.encode("password")).email("jwworker@test.com")
                .role(Role.WORKER).enabled(true).build());

        company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("jwowner@test.com").archived(false).build());

        anotherCompany = companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("anotherjw@test.com").archived(false).build());

        createCompanyMember(company, companyUser, CompanyRole.COMPANY_ADMIN);
        createCompanyMember(anotherCompany, anotherUser, CompanyRole.COMPANY_ADMIN);

        JobTemplate template = jobTemplateRepository.save(JobTemplate.builder()
                .name("Template").company(company).build());

        worker = workerRepository.save(Worker.builder()
                .name("Test Worker").company(company).user(workerUser)
                .email("jwworker@test.com").archived(false).build());

        workflow = workflowRepository.save(Workflow.builder()
                .company(company).name("Test Workflow").build());

        workflowStep = workflowStepRepository.save(WorkflowStep.builder()
                .workflow(workflow).name("Step 1").orderIndex(1).optional(false).build());

        com.workflow.entity.customer.Customer customer = customerRepository.save(
                com.workflow.entity.customer.Customer.builder()
                        .name("Test Customer").company(company).email("customer@test.com").build());

        // Job without workflow (for start tests)
        job = jobRepository.save(Job.builder()
                .template(template).company(company).customer(customer)
                .status(JobStatus.NEW).archived(false).build());

        // Job that already has a workflow
        jobWithWorkflow = jobRepository.save(Job.builder()
                .template(template).company(company).customer(customer)
                .status(JobStatus.NEW).archived(false).build());

        existingJobWorkflow = jobWorkflowRepository.save(JobWorkflow.builder()
                .job(jobWithWorkflow)
                .status(WorkflowStepStatus.NOT_STARTED)
                .build());

        existingJobWorkflowStep = jobWorkflowStepRepository.save(JobWorkflowStep.builder()
                .jobWorkflow(existingJobWorkflow)
                .name("Existing Step")
                .description("A pre-existing step")
                .orderIndex(1)
                .status(WorkflowStepStatus.NOT_STARTED)
                .build());

        companyToken = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken = jwtService.generateToken(workerUser);
    }

    // ============= POST /api/v1/job-workflows/jobs/{jobId}/workflows/{workflowId}/start =============

    @Test
    void shouldStartWorkflowForJobSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/job-workflows/jobs/" + job.getId() + "/workflows/" + workflow.getId() + "/start")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.jobId").value(job.getId()))
                .andExpect(jsonPath("$.steps").isArray())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void shouldReturn404WhenStartingWorkflowForNonExistentJob() throws Exception {
        mockMvc.perform(post("/api/v1/job-workflows/jobs/99999/workflows/" + workflow.getId() + "/start")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenStartingNonExistentWorkflow() throws Exception {
        mockMvc.perform(post("/api/v1/job-workflows/jobs/" + job.getId() + "/workflows/99999/start")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenStartingWorkflowWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/job-workflows/jobs/" + job.getId() + "/workflows/" + workflow.getId() + "/start"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForWorkerRoleOnStartWorkflow() throws Exception {
        // WORKER users have no company context; CompanyRoleAspect rejects with 403
        mockMvc.perform(post("/api/v1/job-workflows/jobs/" + job.getId() + "/workflows/" + workflow.getId() + "/start")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/job-workflows/jobs/{jobId} =============

    @Test
    void shouldGetJobWorkflowByJobIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflows/jobs/" + jobWithWorkflow.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingJobWorkflow.getId()))
                .andExpect(jsonPath("$.jobId").value(jobWithWorkflow.getId()))
                .andExpect(jsonPath("$.steps").isArray());
    }

    @Test
    void shouldReturn409WhenJobWorkflowNotFoundForJob() throws Exception {
        // WorkflowNotStartedException extends ConflictException -> 409
        mockMvc.perform(get("/api/v1/job-workflows/jobs/" + job.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn404WhenAccessingJobWorkflowOfAnotherCompany() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflows/jobs/" + jobWithWorkflow.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/job-workflows/{jobWorkflowId} =============

    @Test
    void shouldGetJobWorkflowByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflows/" + existingJobWorkflow.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingJobWorkflow.getId()))
                .andExpect(jsonPath("$.jobId").value(jobWithWorkflow.getId()));
    }

    @Test
    void shouldReturn404WhenJobWorkflowNotFoundById() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflows/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAccessingAnotherCompanyJobWorkflow() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflows/" + existingJobWorkflow.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/job-workflows =============

    @Test
    void shouldGetAllJobWorkflowsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflows")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(existingJobWorkflow.getId()));
    }

    @Test
    void shouldReturnEmptyListForAnotherCompany() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflows")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ============= PUT /api/v1/job-workflows/{jobWorkflowId} =============

    @Test
    void shouldUpdateJobWorkflowSuccessfully() throws Exception {
        JobWorkflowUpdateRequest request = JobWorkflowUpdateRequest.builder()
                .status(WorkflowStepStatus.STARTED)
                .build();

        mockMvc.perform(put("/api/v1/job-workflows/" + existingJobWorkflow.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingJobWorkflow.getId()));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentJobWorkflow() throws Exception {
        JobWorkflowUpdateRequest request = JobWorkflowUpdateRequest.builder()
                .status(WorkflowStepStatus.COMPLETED).build();

        mockMvc.perform(put("/api/v1/job-workflows/99999")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= PUT /api/v1/job-workflows/{jobWorkflowId}/steps/{stepId} =============

    @Test
    void shouldUpdateJobWorkflowStepSuccessfully() throws Exception {
        JobWorkflowStepUpdateRequest request = JobWorkflowStepUpdateRequest.builder()
                .id(existingJobWorkflowStep.getId())
                .name("Updated Step Name")
                .status(WorkflowStepStatus.STARTED)
                .build();

        mockMvc.perform(put("/api/v1/job-workflows/" + existingJobWorkflow.getId()
                        + "/steps/" + existingJobWorkflowStep.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingJobWorkflowStep.getId()));
    }

    // ============= DELETE /api/v1/job-workflows/jobs/{jobId} =============

    @Test
    void shouldDeleteJobWorkflowByJobIdSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/job-workflows/jobs/" + jobWithWorkflow.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingWorkflowForNonExistentJob() throws Exception {
        mockMvc.perform(delete("/api/v1/job-workflows/jobs/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    // ============= PUT /api/v1/job-workflows/{jobWorkflowId}/assign-worker/{workerId} =============

    @Test
    void shouldAssignWorkerToAllStepsSuccessfully() throws Exception {
        mockMvc.perform(put("/api/v1/job-workflows/" + existingJobWorkflow.getId()
                        + "/assign-worker/" + worker.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingJobWorkflow.getId()));
    }

    @Test
    void shouldReturn404WhenAssigningWorkerToNonExistentJobWorkflow() throws Exception {
        mockMvc.perform(put("/api/v1/job-workflows/99999/assign-worker/" + worker.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    // ============= POST /api/v1/job-workflows/{jobWorkflowId}/steps =============

    @Test
    void shouldAddStepToJobWorkflowSuccessfully() throws Exception {
        JobWorkflowStepCreateRequest request = JobWorkflowStepCreateRequest.builder()
                .name("New Ad-hoc Step")
                .description("Added dynamically")
                .orderIndex(2)
                .status(WorkflowStepStatus.NOT_STARTED)
                .build();

        mockMvc.perform(post("/api/v1/job-workflows/" + existingJobWorkflow.getId() + "/steps")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Ad-hoc Step"));
    }

    @Test
    void shouldReturn404WhenAddingStepToNonExistentJobWorkflow() throws Exception {
        JobWorkflowStepCreateRequest request = JobWorkflowStepCreateRequest.builder()
                .name("Step").orderIndex(1).status(WorkflowStepStatus.NOT_STARTED).build();

        mockMvc.perform(post("/api/v1/job-workflows/99999/steps")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401ForInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflows")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized());
    }
}
