package com.workflow.controller.workflow;

import com.workflow.AbstractControllerIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.dto.workflow.StepVisitLogCreateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.job.JobWorkflow;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.job.JobWorkflowStepVisitLog;
import com.workflow.entity.auth.User;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.customer.CustomerRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.job.JobTemplateRepository;
import com.workflow.repository.job.JobWorkflowRepository;
import com.workflow.repository.job.JobWorkflowStepActivityRepository;
import com.workflow.repository.job.JobWorkflowStepAttachmentRepository;
import com.workflow.repository.job.JobWorkflowStepCommentRepository;
import com.workflow.repository.job.JobWorkflowStepRepository;
import com.workflow.repository.job.JobWorkflowStepVisitLogRepository;
import com.workflow.repository.auth.UserRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JobWorkflowStepVisitLogControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobTemplateRepository jobTemplateRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JobWorkflowRepository jobWorkflowRepository;
    @Autowired private JobWorkflowStepRepository jobWorkflowStepRepository;
    @Autowired private JobWorkflowStepVisitLogRepository visitLogRepository;
    @Autowired private JobWorkflowStepActivityRepository activityRepository;
    @Autowired private JobWorkflowStepCommentRepository commentRepository;
    @Autowired private JobWorkflowStepAttachmentRepository attachmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private JobWorkflowStep step;
    private JobWorkflowStepVisitLog existingVisitLog;
    private User companyUser;
    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        attachmentRepository.deleteAll();
        commentRepository.deleteAll();
        activityRepository.deleteAll();
        visitLogRepository.deleteAll();
        jobWorkflowStepRepository.deleteAll();
        jobWorkflowRepository.deleteAll();
        jobRepository.deleteAll();
        jobTemplateRepository.deleteAll();
        customerRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("visitlogowner")
                .password(passwordEncoder.encode("password")).email("visitlogowner@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anothervisitowner")
                .password(passwordEncoder.encode("password")).email("anothervisit@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("visitworker")
                .password(passwordEncoder.encode("password")).email("visitworker@test.com")
                .role(Role.WORKER).enabled(true).build());

        Company company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("visitlogowner@test.com").archived(false).build());

        Company anotherCompany = companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("anothervisit@test.com").archived(false).build());

        createCompanyMember(company, companyUser, CompanyRole.COMPANY_ADMIN);
        createCompanyMember(anotherCompany, anotherUser, CompanyRole.COMPANY_ADMIN);

        JobTemplate template = jobTemplateRepository.save(JobTemplate.builder()
                .name("Template").company(company).build());

        com.workflow.entity.customer.Customer customer = customerRepository.save(
                com.workflow.entity.customer.Customer.builder()
                        .name("Test Customer").company(company).email("customer@test.com").build());

        Job job = jobRepository.save(Job.builder()
                .template(template).company(company).customer(customer)
                .status(JobStatus.NEW).archived(false).build());

        JobWorkflow jobWorkflow = jobWorkflowRepository.save(JobWorkflow.builder()
                .job(job).status(WorkflowStepStatus.STARTED).build());

        step = jobWorkflowStepRepository.save(JobWorkflowStep.builder()
                .jobWorkflow(jobWorkflow)
                .name("Test Step")
                .orderIndex(1)
                .status(WorkflowStepStatus.STARTED)
                .build());

        existingVisitLog = visitLogRepository.save(JobWorkflowStepVisitLog.builder()
                .step(step)
                .loggedBy(companyUser)
                .visitDate(LocalDate.of(2024, 6, 15))
                .timeIn(LocalTime.of(9, 0))
                .timeOut(LocalTime.of(11, 0))
                .description("Morning visit")
                .build());

        companyToken = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken = jwtService.generateToken(workerUser);
    }

    // ============= POST /api/v1/job-workflow-steps/{stepId}/visits =============

    @Test
    void shouldAddVisitLogSuccessfully() throws Exception {
        StepVisitLogCreateRequest request = new StepVisitLogCreateRequest();
        request.setVisitDate(LocalDate.of(2024, 7, 10));
        request.setTimeIn(LocalTime.of(8, 30));
        request.setTimeOut(LocalTime.of(12, 0));
        request.setDescription("Full morning session");
        request.setLoggedById(companyUser.getId());

        mockMvc.perform(post("/api/v1/job-workflow-steps/" + step.getId() + "/visits")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.visitDate").value("2024-07-10"))
                .andExpect(jsonPath("$.timeIn").value("08:30:00"))
                .andExpect(jsonPath("$.description").value("Full morning session"))
                .andExpect(jsonPath("$.loggedById").value(companyUser.getId()));
    }

    @Test
    void shouldAddVisitLogWithoutTimeOut() throws Exception {
        StepVisitLogCreateRequest request = new StepVisitLogCreateRequest();
        request.setVisitDate(LocalDate.of(2024, 7, 11));
        request.setTimeIn(LocalTime.of(14, 0));
        request.setLoggedById(companyUser.getId());

        mockMvc.perform(post("/api/v1/job-workflow-steps/" + step.getId() + "/visits")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.timeOut").isEmpty());
    }

    @Test
    void shouldReturn404WhenAddingVisitLogToNonExistentStep() throws Exception {
        StepVisitLogCreateRequest request = new StepVisitLogCreateRequest();
        request.setVisitDate(LocalDate.now());
        request.setTimeIn(LocalTime.of(9, 0));
        request.setLoggedById(companyUser.getId());

        mockMvc.perform(post("/api/v1/job-workflow-steps/99999/visits")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenAddingVisitLogWithoutToken() throws Exception {
        StepVisitLogCreateRequest request = new StepVisitLogCreateRequest();
        request.setVisitDate(LocalDate.now());
        request.setTimeIn(LocalTime.of(9, 0));

        mockMvc.perform(post("/api/v1/job-workflow-steps/" + step.getId() + "/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForWorkerRoleOnAddVisitLog() throws Exception {
        // WORKER users have no company context; CompanyRoleAspect rejects with 403
        StepVisitLogCreateRequest request = new StepVisitLogCreateRequest();
        request.setVisitDate(LocalDate.now());
        request.setTimeIn(LocalTime.of(9, 0));
        request.setLoggedById(companyUser.getId());

        mockMvc.perform(post("/api/v1/job-workflow-steps/" + step.getId() + "/visits")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= PUT /api/v1/job-workflow-steps/visits/{visitLogId} =============

    @Test
    void shouldUpdateVisitLogSuccessfully() throws Exception {
        StepVisitLogCreateRequest request = new StepVisitLogCreateRequest();
        request.setVisitDate(LocalDate.of(2024, 6, 16));
        request.setTimeIn(LocalTime.of(10, 0));
        request.setTimeOut(LocalTime.of(14, 30));
        request.setDescription("Updated description");
        request.setLoggedById(companyUser.getId());

        mockMvc.perform(put("/api/v1/job-workflow-steps/visits/" + existingVisitLog.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingVisitLog.getId()))
                .andExpect(jsonPath("$.visitDate").value("2024-06-16"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentVisitLog() throws Exception {
        StepVisitLogCreateRequest request = new StepVisitLogCreateRequest();
        request.setVisitDate(LocalDate.now());
        request.setTimeIn(LocalTime.of(9, 0));
        request.setLoggedById(companyUser.getId());

        mockMvc.perform(put("/api/v1/job-workflow-steps/visits/99999")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenUpdatingAnotherCompanyVisitLog() throws Exception {
        // ForbiddenActionException: company ownership check fails -> 403
        StepVisitLogCreateRequest request = new StepVisitLogCreateRequest();
        request.setVisitDate(LocalDate.now());
        request.setTimeIn(LocalTime.of(9, 0));

        mockMvc.perform(put("/api/v1/job-workflow-steps/visits/" + existingVisitLog.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= DELETE /api/v1/job-workflow-steps/visits/{visitLogId} =============

    @Test
    void shouldDeleteVisitLogSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/job-workflow-steps/visits/" + existingVisitLog.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentVisitLog() throws Exception {
        mockMvc.perform(delete("/api/v1/job-workflow-steps/visits/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenDeletingAnotherCompanyVisitLog() throws Exception {
        // ForbiddenActionException: company ownership check fails -> 403
        mockMvc.perform(delete("/api/v1/job-workflow-steps/visits/" + existingVisitLog.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/job-workflow-steps/{stepId}/visits =============

    @Test
    void shouldGetVisitLogsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/visits")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitLogs").isArray())
                .andExpect(jsonPath("$.visitLogs", hasSize(1)))
                .andExpect(jsonPath("$.visitLogs[0].id").value(existingVisitLog.getId()))
                .andExpect(jsonPath("$.visitLogs[0].visitDate").value("2024-06-15"))
                .andExpect(jsonPath("$.totalWorkedMinutes").exists());
    }

    @Test
    void shouldReturn403WhenGettingVisitLogsForAnotherCompanyStep() throws Exception {
        // getStep() throws UnauthorizedWorkflowAccessException (ForbiddenException) -> 403
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/visits")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenGettingVisitLogsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/visits"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401ForInvalidTokenOnVisitLogs() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/visits")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized());
    }
}
