package com.workflow.controller.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.constant.workflow.StepDiscussionType;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.dto.workflow.StepAttachmentUpdateRequest;
import com.workflow.dto.workflow.StepCommentCreateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.job.JobWorkflow;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.job.JobWorkflowStepAttachment;
import com.workflow.entity.job.JobWorkflowStepComment;
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
import com.workflow.service.storage.IStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JobWorkflowStepActivityControllerIntegrationTest {

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
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @MockBean private IStorageService storageService;

    private Company company;
    private Company anotherCompany;
    private JobWorkflowStep step;
    private JobWorkflowStepComment existingComment;
    private JobWorkflowStepAttachment existingAttachment;
    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        activityRepository.deleteAll();
        attachmentRepository.deleteAll();
        commentRepository.deleteAll();
        jobWorkflowStepRepository.deleteAll();
        jobWorkflowRepository.deleteAll();
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
                .uuid(UUID.randomUUID().toString()).username("stepactowner")
                .password(passwordEncoder.encode("password")).email("stepactowner@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anotherstepowner")
                .password(passwordEncoder.encode("password")).email("anotherstep@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("stepworker")
                .password(passwordEncoder.encode("password")).email("stepworker@test.com")
                .role(Role.WORKER).enabled(true).build());

        company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("stepactowner@test.com").archived(false).build());

        anotherCompany = companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("anotherstep@test.com").archived(false).build());

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

        existingComment = commentRepository.save(JobWorkflowStepComment.builder()
                .step(step)
                .author(companyUser)
                .content("Initial comment")
                .type(StepDiscussionType.GENERAL)
                .build());

        existingAttachment = attachmentRepository.save(JobWorkflowStepAttachment.builder()
                .step(step)
                .uploadedBy(companyUser)
                .type(StepDiscussionType.GENERAL)
                .fileName("test-file.pdf")
                .fileType("application/pdf")
                .fileUrl("attachments/test-file.pdf")
                .description("A test attachment")
                .build());

        companyToken = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken = jwtService.generateToken(workerUser);
    }

    // ============= POST /api/v1/job-workflow-steps/{stepId}/comments =============

    @Test
    void shouldAddCommentSuccessfully() throws Exception {
        StepCommentCreateRequest request = new StepCommentCreateRequest();
        request.setContent("New comment content");
        request.setType(StepDiscussionType.GENERAL);

        mockMvc.perform(post("/api/v1/job-workflow-steps/" + step.getId() + "/comments")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("New comment content"))
                .andExpect(jsonPath("$.type").value("GENERAL"))
                .andExpect(jsonPath("$.authorId").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn400WhenCommentContentIsBlank() throws Exception {
        StepCommentCreateRequest request = new StepCommentCreateRequest();
        request.setContent("");
        request.setType(StepDiscussionType.GENERAL);

        mockMvc.perform(post("/api/v1/job-workflow-steps/" + step.getId() + "/comments")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenAddingCommentToNonExistentStep() throws Exception {
        StepCommentCreateRequest request = new StepCommentCreateRequest();
        request.setContent("Comment");
        request.setType(StepDiscussionType.GENERAL);

        mockMvc.perform(post("/api/v1/job-workflow-steps/99999/comments")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenAddingCommentWithoutToken() throws Exception {
        // No auth header → Spring Security rejects with 403 (anonymous user)
        StepCommentCreateRequest request = new StepCommentCreateRequest();
        request.setContent("Comment text");
        request.setType(StepDiscussionType.GENERAL);

        mockMvc.perform(post("/api/v1/job-workflow-steps/" + step.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= PUT /api/v1/job-workflow-steps/comments/{commentId} =============

    @Test
    void shouldUpdateCommentSuccessfully() throws Exception {
        StepCommentCreateRequest request = new StepCommentCreateRequest();
        request.setContent("Updated comment content");
        request.setType(StepDiscussionType.INTERNAL_NOTE);

        mockMvc.perform(put("/api/v1/job-workflow-steps/comments/" + existingComment.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingComment.getId()))
                .andExpect(jsonPath("$.content").value("Updated comment content"))
                .andExpect(jsonPath("$.type").value("INTERNAL_NOTE"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentComment() throws Exception {
        StepCommentCreateRequest request = new StepCommentCreateRequest();
        request.setContent("Content");
        request.setType(StepDiscussionType.GENERAL);

        mockMvc.perform(put("/api/v1/job-workflow-steps/comments/99999")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenUpdatingCommentFromAnotherCompany() throws Exception {
        // ForbiddenActionException: author check fails -> 403
        StepCommentCreateRequest request = new StepCommentCreateRequest();
        request.setContent("Updated");
        request.setType(StepDiscussionType.GENERAL);

        mockMvc.perform(put("/api/v1/job-workflow-steps/comments/" + existingComment.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= DELETE /api/v1/job-workflow-steps/comments/{commentId} =============

    @Test
    void shouldDeleteCommentSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/job-workflow-steps/comments/" + existingComment.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentComment() throws Exception {
        mockMvc.perform(delete("/api/v1/job-workflow-steps/comments/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/job-workflow-steps/{stepId}/comments =============

    @Test
    void shouldGetCommentsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/comments")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(existingComment.getId()))
                .andExpect(jsonPath("$[0].content").value("Initial comment"));
    }

    @Test
    void shouldReturn403WhenGettingCommentsForAnotherCompanyStep() throws Exception {
        // getStep() throws UnauthorizedWorkflowAccessException (ForbiddenException) -> 403
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/comments")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenGettingCommentsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/comments"))
                .andExpect(status().isForbidden());
    }

    // ============= POST /api/v1/job-workflow-steps/{stepId}/attachments =============

    @Test
    void shouldUploadAttachmentSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-doc.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/job-workflow-steps/" + step.getId() + "/attachments")
                        .file(file)
                        .param("type", "GENERAL")
                        .param("description", "Test document")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fileName").value("test-doc.pdf"))
                .andExpect(jsonPath("$.type").value("GENERAL"))
                .andExpect(jsonPath("$.description").value("Test document"));
    }

    @Test
    void shouldReturn404WhenUploadingAttachmentToNonExistentStep() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/job-workflow-steps/99999/attachments")
                        .file(file)
                        .param("type", "GENERAL")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    // ============= PUT /api/v1/job-workflow-steps/attachments/{attachmentId} =============

    @Test
    void shouldUpdateAttachmentSuccessfully() throws Exception {
        StepAttachmentUpdateRequest request = new StepAttachmentUpdateRequest();
        request.setFileName("renamed-file.pdf");
        request.setDescription("Updated description");
        request.setType(StepDiscussionType.INTERNAL_NOTE);

        mockMvc.perform(put("/api/v1/job-workflow-steps/attachments/" + existingAttachment.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingAttachment.getId()))
                .andExpect(jsonPath("$.fileName").value("renamed-file.pdf"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentAttachment() throws Exception {
        StepAttachmentUpdateRequest request = new StepAttachmentUpdateRequest();
        request.setFileName("file.pdf");

        mockMvc.perform(put("/api/v1/job-workflow-steps/attachments/99999")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= DELETE /api/v1/job-workflow-steps/attachments/{attachmentId} =============

    @Test
    void shouldDeleteAttachmentSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/job-workflow-steps/attachments/" + existingAttachment.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentAttachment() throws Exception {
        mockMvc.perform(delete("/api/v1/job-workflow-steps/attachments/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/job-workflow-steps/{stepId}/attachments =============

    @Test
    void shouldGetAttachmentsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/attachments")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(existingAttachment.getId()))
                .andExpect(jsonPath("$[0].fileName").value("test-file.pdf"));
    }

    @Test
    void shouldReturn403WhenGettingAttachmentsForAnotherCompanyStep() throws Exception {
        // getStep() throws UnauthorizedWorkflowAccessException (ForbiddenException) -> 403
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/attachments")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/job-workflow-steps/{stepId}/discussion =============

    @Test
    void shouldGetDiscussionTimelineSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/discussion")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturn401ForInvalidTokenOnComments() throws Exception {
        mockMvc.perform(get("/api/v1/job-workflow-steps/" + step.getId() + "/comments")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized());
    }
}
