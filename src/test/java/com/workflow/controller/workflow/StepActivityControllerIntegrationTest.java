package com.workflow.controller.workflow;

import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.entity.company.Company;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.job.JobWorkflow;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.job.JobWorkflowStepActivity;
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
import com.workflow.repository.auth.UserRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StepActivityControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobTemplateRepository jobTemplateRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JobWorkflowRepository jobWorkflowRepository;
    @Autowired private JobWorkflowStepRepository jobWorkflowStepRepository;
    @Autowired private JobWorkflowStepActivityRepository activityRepository;
    @Autowired private JobWorkflowStepCommentRepository commentRepository;
    @Autowired private JobWorkflowStepAttachmentRepository attachmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private JobWorkflowStep step;
    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        attachmentRepository.deleteAll();
        commentRepository.deleteAll();
        activityRepository.deleteAll();
        jobWorkflowStepRepository.deleteAll();
        jobWorkflowRepository.deleteAll();
        jobRepository.deleteAll();
        jobTemplateRepository.deleteAll();
        customerRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        User companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("timelineowner")
                .password(passwordEncoder.encode("password")).email("timelineowner@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anothertimelineowner")
                .password(passwordEncoder.encode("password")).email("anothertimeline@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("timelineworker")
                .password(passwordEncoder.encode("password")).email("timelineworker@test.com")
                .role(Role.WORKER).enabled(true).build());

        Company company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("timelineowner@test.com").archived(false).build());

        companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("anothertimeline@test.com").archived(false).build());

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

        // Add some activity entries
        activityRepository.save(JobWorkflowStepActivity.builder()
                .step(step)
                .actor(companyUser)
                .type(JobWorkflowStepActivityType.STATUS_CHANGED)
                .message("Step started")
                .build());

        activityRepository.save(JobWorkflowStepActivity.builder()
                .step(step)
                .actor(companyUser)
                .type(JobWorkflowStepActivityType.COMMENT)
                .message("Comment added")
                .build());

        companyToken = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken = jwtService.generateToken(workerUser);
    }

    // ============= GET /api/v1/job-workflow-steps/{stepId}/timeline =============

    @Test
    void shouldGetTimelineSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/timeline")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].message").exists())
                .andExpect(jsonPath("$[0].actorId").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    void shouldReturn403WhenGettingTimelineForAnotherCompanyStep() throws Exception {
        // getStep() throws UnauthorizedWorkflowAccessException (ForbiddenException) -> 403
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/timeline")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenStepNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/99999/timeline")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenGettingTimelineWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/timeline"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401ForInvalidTokenOnTimeline() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/timeline")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn404ForWorkerRoleOnTimelineBecauseNoCompany() throws Exception {
        // /api/v1/job-workflow-steps/** is not restricted to COMPANY role in SecurityConfig;
        // getCompanyId() throws CompanyNotFoundException (404) for worker users
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/timeline")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isNotFound());
    }
}
