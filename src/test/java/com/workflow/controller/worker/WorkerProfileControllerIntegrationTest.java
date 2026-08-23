package com.workflow.controller.worker;

import com.workflow.AbstractControllerIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.dto.worker.WorkerRateUpdateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.customer.Customer;
import com.workflow.entity.auth.User;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.job.JobWorkflow;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.job.JobWorkflowStepVisitLog;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.customer.CustomerRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.job.JobTemplateRepository;
import com.workflow.repository.job.JobWorkflowRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkerProfileControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JobTemplateRepository jobTemplateRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobWorkflowRepository jobWorkflowRepository;
    @Autowired private JobWorkflowStepRepository jobWorkflowStepRepository;
    @Autowired private JobWorkflowStepVisitLogRepository visitLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Worker worker1;
    private String companyAdminToken;
    private String worker1Token;

    @BeforeEach
    void setUp() {
        visitLogRepository.deleteAll();
        jobWorkflowStepRepository.deleteAll();
        jobWorkflowRepository.deleteAll();
        jobRepository.deleteAll();
        jobTemplateRepository.deleteAll();
        customerRepository.deleteAll();
        workerRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        when(storageService.resolveFileUrl(anyString())).thenReturn("https://fake-s3.test/photo.png");
        doNothing().when(storageService).upload(anyString(), any(), anyLong(), anyString());
        doNothing().when(storageService).delete(anyString());

        User companyAdmin = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("profilecompanyadmin")
                .password(passwordEncoder.encode("password")).email("profileadmin@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User worker1User = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("profileworker1")
                .password(passwordEncoder.encode("password")).email("profileworker1@test.com")
                .role(Role.WORKER).enabled(true).build());

        Company company = companyRepository.save(Company.builder()
                .name("Profile Test Company").user(companyAdmin).email("profileadmin@test.com").archived(false).build());

        createCompanyMember(company, companyAdmin, CompanyRole.COMPANY_ADMIN);

        worker1 = workerRepository.save(Worker.builder()
                .name("Profile Worker One").company(company).user(worker1User).archived(false).build());

        JobTemplate template = jobTemplateRepository.save(JobTemplate.builder()
                .name("Template").company(company).build());
        Customer customer = customerRepository.save(Customer.builder()
                .name("Test Customer").company(company).email("customer@test.com").build());
        Job job = jobRepository.save(Job.builder()
                .template(template).company(company).customer(customer)
                .status(JobStatus.NEW).archived(false).build());
        JobWorkflow jobWorkflow = jobWorkflowRepository.save(JobWorkflow.builder()
                .job(job).status(WorkflowStepStatus.STARTED).build());
        JobWorkflowStep step = jobWorkflowStepRepository.save(JobWorkflowStep.builder()
                .jobWorkflow(jobWorkflow).name("Step").orderIndex(1).status(WorkflowStepStatus.STARTED).build());

        // A completed 8-hour visit within the target week (Mon 2026-08-03 - Sun 2026-08-09)
        visitLogRepository.save(JobWorkflowStepVisitLog.builder()
                .step(step).loggedBy(worker1User)
                .visitDate(LocalDate.of(2026, 8, 5))
                .timeIn(LocalTime.of(9, 0)).timeOut(LocalTime.of(17, 0))
                .build());

        companyAdminToken = jwtService.generateToken(companyAdmin);
        worker1Token = jwtService.generateToken(worker1User);
    }

    // ============= GET /api/v1/worker/profile (self) =============

    @Test
    void shouldGetOwnProfileWithoutHourlyRate() throws Exception {
        mockMvc.perform(get("/api/v1/worker/profile")
                        .header("Authorization", "Bearer " + worker1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Profile Worker One"))
                .andExpect(jsonPath("$.hourlyRate").doesNotExist());
    }

    // ============= POST /api/v1/worker/profile/photo (self) =============

    @Test
    void shouldUploadOwnPhotoSuccessfully() throws Exception {
        byte[] fileContent = "content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "me.png", "image/png", fileContent);

        Long companyId = worker1.getCompany().getId();
        long storageUsedBeforeUpload = companyRepository.findById(companyId).orElseThrow().getStorageUsedBytes();

        mockMvc.perform(multipart("/api/v1/worker/profile/photo")
                        .file(file)
                        .header("Authorization", "Bearer " + worker1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").exists());

        long storageUsedAfterUpload = companyRepository.findById(companyId).orElseThrow().getStorageUsedBytes();
        assertThat(storageUsedAfterUpload - storageUsedBeforeUpload).isEqualTo(fileContent.length);
    }

    // No companion delete-path test here: replacing a photo decrements the old photo's bytes inside
    // WorkerService.uploadPhoto()'s afterCommit callback (registered via TransactionSynchronizationManager).
    // This test class runs under @Transactional, which rolls back at the end of every test instead of
    // committing — so afterCommit() never fires and the decrement is unobservable in this test setup.
    // Pre-existing test-infra limitation (same reason WorkerService's old-photo S3 delete is also
    // unverifiable here), not something to work around.

    // ============= PATCH /api/v1/workers/{id}/rate (admin only, not worker-writable) =============

    @Test
    void shouldUpdateHourlyRateAsAdmin() throws Exception {
        WorkerRateUpdateRequest request = new WorkerRateUpdateRequest(new BigDecimal("18.75"));

        mockMvc.perform(patch("/api/v1/workers/" + worker1.getId() + "/rate")
                        .header("Authorization", "Bearer " + companyAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hourlyRate").value(18.75));
    }

    @Test
    void shouldReturn403WhenWorkerTriesToSetOwnRate() throws Exception {
        WorkerRateUpdateRequest request = new WorkerRateUpdateRequest(new BigDecimal("999.00"));

        mockMvc.perform(patch("/api/v1/workers/" + worker1.getId() + "/rate")
                        .header("Authorization", "Bearer " + worker1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= GET weekly hours (self + admin) =============

    @Test
    void shouldGetOwnWeeklyHours() throws Exception {
        mockMvc.perform(get("/api/v1/worker/profile/hours/weekly")
                        .param("date", "2026-08-05")
                        .header("Authorization", "Bearer " + worker1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHours").value(8.00))
                .andExpect(jsonPath("$.hasOpenVisit").value(false))
                .andExpect(jsonPath("$.weekStart").value("2026-08-03"))
                .andExpect(jsonPath("$.weekEnd").value("2026-08-09"));
    }

    @Test
    void shouldGetWeeklyHoursForWorkerAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/workers/" + worker1.getId() + "/hours/weekly")
                        .param("date", "2026-08-05")
                        .header("Authorization", "Bearer " + companyAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHours").value(8.00));
    }
}
