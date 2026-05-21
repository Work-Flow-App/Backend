package com.workflow.controller.workflow;

import com.workflow.AbstractControllerIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.constant.workflow.StepDiscussionType;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.dto.workflow.StepCommentCreateRequest;
import com.workflow.dto.workflow.StepVisitLogCreateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.job.JobWorkflow;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.job.JobWorkflowStepComment;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.Worker;
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
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkerJobWorkflowControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobTemplateRepository jobTemplateRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JobWorkflowRepository jobWorkflowRepository;
    @Autowired private JobWorkflowStepRepository jobWorkflowStepRepository;
    @Autowired private JobWorkflowStepCommentRepository commentRepository;
    @Autowired private JobWorkflowStepAttachmentRepository attachmentRepository;
    @Autowired private JobWorkflowStepActivityRepository activityRepository;
    @Autowired private JobWorkflowStepVisitLogRepository visitLogRepository;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;


    private User workerUser;
    private User anotherWorkerUser;
    private Worker worker;
    private JobWorkflow jobWorkflow;
    private JobWorkflowStep assignedStep;
    private JobWorkflowStep unassignedStep;
    private JobWorkflowStepComment stepComment;
    private String workerToken;
    private String anotherWorkerToken;
    private String companyToken;

    @BeforeEach
    void setUp() {
        attachmentRepository.deleteAll();
        commentRepository.deleteAll();
        activityRepository.deleteAll();
        visitLogRepository.deleteAll();
        jobWorkflowStepRepository.deleteAll();
        jobWorkflowRepository.deleteAll();
        workerRepository.deleteAll();
        jobRepository.deleteAll();
        jobTemplateRepository.deleteAll();
        customerRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        when(storageService.generatePresignedUrl(anyString())).thenReturn("https://fake-s3.test/file.jpg");
        when(storageService.resolveFileUrl(anyString())).thenReturn("https://fake-s3.test/file.jpg");
        doNothing().when(storageService).upload(anyString(), any(), anyLong(), anyString());
        doNothing().when(storageService).delete(anyString());

        User companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("workerworkflowco")
                .password(passwordEncoder.encode("password")).email("workerworkflowco@test.com")
                .role(Role.COMPANY).enabled(true).build());

        workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("assignedworker1")
                .password(passwordEncoder.encode("password")).email("assignedworker1@test.com")
                .role(Role.WORKER).enabled(true).build());

        anotherWorkerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("unassignedworker2")
                .password(passwordEncoder.encode("password")).email("unassignedworker2@test.com")
                .role(Role.WORKER).enabled(true).build());

        Company company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("workerworkflowco@test.com").archived(false).build());

        createCompanyMember(company, companyUser, CompanyRole.COMPANY_ADMIN);

        worker = workerRepository.save(Worker.builder()
                .name("Assigned Worker").company(company).user(workerUser)
                .email("assignedworker1@test.com").archived(false).build());

        // Save but don't assign to any steps
        workerRepository.save(Worker.builder()
                .name("Unassigned Worker").company(company).user(anotherWorkerUser)
                .email("unassignedworker2@test.com").archived(false).build());

        JobTemplate template = jobTemplateRepository.save(JobTemplate.builder()
                .name("Template").company(company).build());

        com.workflow.entity.customer.Customer customer = customerRepository.save(
                com.workflow.entity.customer.Customer.builder()
                        .name("Test Customer").company(company).email("customer@test.com").build());

        Job job = jobRepository.save(Job.builder()
                .template(template).company(company).customer(customer)
                .status(JobStatus.NEW).archived(false).build());

        jobWorkflow = jobWorkflowRepository.save(JobWorkflow.builder()
                .job(job).status(WorkflowStepStatus.STARTED).build());

        // Step assigned to workerUser
        assignedStep = jobWorkflowStepRepository.save(JobWorkflowStep.builder()
                .jobWorkflow(jobWorkflow)
                .name("Assigned Step")
                .orderIndex(1)
                .status(WorkflowStepStatus.NOT_STARTED)
                .assignedWorkers(new java.util.HashSet<>(Set.of(worker)))
                .build());

        // Step NOT assigned to any worker
        unassignedStep = jobWorkflowStepRepository.save(JobWorkflowStep.builder()
                .jobWorkflow(jobWorkflow)
                .name("Unassigned Step")
                .orderIndex(2)
                .status(WorkflowStepStatus.NOT_STARTED)
                .build());

        stepComment = commentRepository.save(JobWorkflowStepComment.builder()
                .step(assignedStep)
                .author(workerUser)
                .content("Worker comment")
                .type(StepDiscussionType.GENERAL)
                .build());

        workerToken = jwtService.generateToken(workerUser);
        anotherWorkerToken = jwtService.generateToken(anotherWorkerUser);
        companyToken = jwtService.generateToken(companyUser);
    }

    // ============= GET /api/v1/worker/job-workflows =============

    @Test
    void shouldGetAssignedJobWorkflowsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflows")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(jobWorkflow.getId()));
    }

    @Test
    void shouldReturnEmptyListForWorkerWithNoAssignments() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflows")
                        .header("Authorization", "Bearer " + anotherWorkerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn403ForCompanyRoleOnWorkerEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflows")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForMissingTokenOnWorkerEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflows"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401ForInvalidTokenOnWorkerEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflows")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized());
    }

    // ============= GET /api/v1/worker/job-workflows/{jobWorkflowId} =============

    @Test
    void shouldGetJobWorkflowIfAssignedSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflows/" + jobWorkflow.getId())
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobWorkflow.getId()))
                .andExpect(jsonPath("$.jobId").exists());
    }

    @Test
    void shouldReturn403WhenWorkerNotAssignedToJobWorkflow() throws Exception {
        // anotherWorkerUser has no assigned steps in this workflow
        mockMvc.perform(get("/api/v1/worker/job-workflows/" + jobWorkflow.getId())
                        .header("Authorization", "Bearer " + anotherWorkerToken))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/worker/job-workflow-steps/{stepId} =============

    @Test
    void shouldGetAssignedStepSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflow-steps/" + assignedStep.getId())
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assignedStep.getId()))
                .andExpect(jsonPath("$.name").value("Assigned Step"));
    }

    @Test
    void shouldReturn403WhenWorkerNotAssignedToStep() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflow-steps/" + unassignedStep.getId())
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/worker/job-workflow-steps =============

    @Test
    void shouldGetAllAssignedStepsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflow-steps")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldReturnEmptyStepsForUnassignedWorker() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflow-steps")
                        .header("Authorization", "Bearer " + anotherWorkerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ============= POST /api/v1/worker/job-workflow-steps/{stepId}/start =============

    @Test
    void shouldStartStepSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/worker/job-workflow-steps/" + assignedStep.getId() + "/start")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assignedStep.getId()))
                .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    void shouldReturn403WhenStartingUnassignedStep() throws Exception {
        mockMvc.perform(post("/api/v1/worker/job-workflow-steps/" + unassignedStep.getId() + "/start")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    // ============= POST /api/v1/worker/job-workflow-steps/{stepId}/complete =============

    @Test
    void shouldCompleteStepSuccessfully() throws Exception {
        // First start the step
        mockMvc.perform(post("/api/v1/worker/job-workflow-steps/" + assignedStep.getId() + "/start")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/worker/job-workflow-steps/" + assignedStep.getId() + "/complete")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assignedStep.getId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldReturn403WhenCompletingUnassignedStep() throws Exception {
        mockMvc.perform(post("/api/v1/worker/job-workflow-steps/" + unassignedStep.getId() + "/complete")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/worker/job-workflow-steps/{stepId}/comments =============

    @Test
    void shouldGetStepCommentsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflow-steps/" + assignedStep.getId() + "/comments")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Worker comment"));
    }

    @Test
    void shouldReturn403WhenGettingCommentsForUnassignedStep() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflow-steps/" + unassignedStep.getId() + "/comments")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    // ============= POST /api/v1/worker/job-workflow-steps/{stepId}/comments =============

    @Test
    void shouldAddCommentToAssignedStepSuccessfully() throws Exception {
        StepCommentCreateRequest request = new StepCommentCreateRequest();
        request.setContent("Worker added comment");
        request.setType(StepDiscussionType.GENERAL);

        mockMvc.perform(post("/api/v1/worker/job-workflow-steps/" + assignedStep.getId() + "/comments")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("Worker added comment"))
                .andExpect(jsonPath("$.type").value("GENERAL"));
    }

    @Test
    void shouldReturn400WhenAddingCommentWithBlankContent() throws Exception {
        StepCommentCreateRequest request = new StepCommentCreateRequest();
        request.setContent("");
        request.setType(StepDiscussionType.GENERAL);

        mockMvc.perform(post("/api/v1/worker/job-workflow-steps/" + assignedStep.getId() + "/comments")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ============= GET /api/v1/worker/job-workflow-steps/{stepId}/attachments =============

    @Test
    void shouldGetAttachmentsForAssignedStepSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflow-steps/" + assignedStep.getId() + "/attachments")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ============= POST /api/v1/worker/job-workflow-steps/{stepId}/attachments =============

    @Test
    void shouldUploadAttachmentToAssignedStepSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "worker-doc.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/worker/job-workflow-steps/" + assignedStep.getId() + "/attachments")
                        .file(file)
                        .param("type", "GENERAL")
                        .param("description", "Worker attachment")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fileName").value("worker-doc.pdf"));
    }

    // ============= GET /api/v1/worker/job-workflow-steps/{stepId}/discussion =============

    @Test
    void shouldGetDiscussionForAssignedStepSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflow-steps/" + assignedStep.getId() + "/discussion")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ============= POST /api/v1/worker/job-workflow-steps/{stepId}/visits =============

    @Test
    void shouldAddVisitLogToAssignedStepSuccessfully() throws Exception {
        StepVisitLogCreateRequest request = new StepVisitLogCreateRequest();
        request.setVisitDate(LocalDate.now());
        request.setTimeIn(LocalTime.of(9, 0));
        request.setTimeOut(LocalTime.of(12, 0));
        request.setDescription("Morning work session");
        request.setLoggedById(workerUser.getId());

        mockMvc.perform(post("/api/v1/worker/job-workflow-steps/" + assignedStep.getId() + "/visits")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.timeIn").value("09:00:00"));
    }

    // ============= GET /api/v1/worker/job-workflow-steps/{stepId}/visits =============

    @Test
    void shouldGetVisitLogsForAssignedStepSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflow-steps/" + assignedStep.getId() + "/visits")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitLogs").isArray());
    }

    @Test
    void shouldReturn403WhenGettingVisitLogsForUnassignedStep() throws Exception {
        mockMvc.perform(get("/api/v1/worker/job-workflow-steps/" + unassignedStep.getId() + "/visits")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }
}
