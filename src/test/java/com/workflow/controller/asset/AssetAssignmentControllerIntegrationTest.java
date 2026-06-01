package com.workflow.controller.asset;

import com.workflow.AbstractControllerIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.dto.asset.AssetAssignmentCreateRequest;
import com.workflow.dto.asset.AssetAssignmentReturnRequest;
import com.workflow.entity.asset.Asset;
import com.workflow.entity.asset.AssetJobAssignment;
import com.workflow.entity.company.Company;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.asset.AssetJobAssignmentRepository;
import com.workflow.repository.asset.AssetRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.customer.CustomerRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.job.JobTemplateRepository;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AssetAssignmentControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private AssetRepository assetRepository;
    @Autowired private AssetJobAssignmentRepository assignmentRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobTemplateRepository jobTemplateRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Company company;
    private Company anotherCompany;
    private Asset availableAsset;
    private Asset anotherCompanyAsset;
    private Job job;
    private Worker worker;
    private AssetJobAssignment existingAssignment;
    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        assetRepository.deleteAll();
        workerRepository.deleteAll();
        jobRepository.deleteAll();
        jobTemplateRepository.deleteAll();
        customerRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        User companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("assignmentowner")
                .password(passwordEncoder.encode("password")).email("assignmentowner@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anotherassignowner")
                .password(passwordEncoder.encode("password")).email("anotherassign@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("assignworker")
                .password(passwordEncoder.encode("password")).email("assignworker@test.com")
                .role(Role.WORKER).enabled(true).build());

        company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("assignmentowner@test.com").archived(false).build());

        anotherCompany = companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("anotherassign@test.com").archived(false).build());

        createCompanyMember(company, companyUser, CompanyRole.COMPANY_ADMIN);
        createCompanyMember(anotherCompany, anotherUser, CompanyRole.COMPANY_ADMIN);

        availableAsset = assetRepository.save(Asset.builder()
                .company(company)
                .name("Available Laptop")
                .purchasePrice(new BigDecimal("1000.00"))
                .purchaseDate(LocalDate.of(2023, 1, 1))
                .depreciationRate(new BigDecimal("20.00"))
                .available(true)
                .archived(false)
                .build());

        anotherCompanyAsset = assetRepository.save(Asset.builder()
                .company(anotherCompany)
                .name("Another Company Asset")
                .purchasePrice(new BigDecimal("500.00"))
                .purchaseDate(LocalDate.of(2023, 1, 1))
                .depreciationRate(new BigDecimal("10.00"))
                .available(true)
                .archived(false)
                .build());

        JobTemplate template = jobTemplateRepository.save(JobTemplate.builder()
                .name("Template").company(company).build());

        com.workflow.entity.customer.Customer customer = customerRepository.save(
                com.workflow.entity.customer.Customer.builder()
                        .name("Test Customer").company(company).email("customer@test.com").build());

        job = jobRepository.save(Job.builder()
                .template(template).company(company).customer(customer)
                .status(JobStatus.NEW).archived(false).build());

        worker = workerRepository.save(Worker.builder()
                .name("Test Worker").company(company).user(workerUser)
                .email("assignworker@test.com").archived(false).build());

        // Create an already-assigned asset for return tests
        Asset assignedAsset = assetRepository.save(Asset.builder()
                .company(company)
                .name("Assigned Asset")
                .purchasePrice(new BigDecimal("800.00"))
                .purchaseDate(LocalDate.of(2023, 1, 1))
                .depreciationRate(new BigDecimal("15.00"))
                .available(false)
                .archived(false)
                .build());

        existingAssignment = assignmentRepository.save(AssetJobAssignment.builder()
                .asset(assignedAsset)
                .job(job)
                .assignedWorker(worker)
                .notes("Test assignment")
                .assignedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());

        companyToken = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken = jwtService.generateToken(workerUser);
    }

    // ============= POST /api/v1/asset-assignments/assign =============

    @Test
    void shouldAssignAssetSuccessfully() throws Exception {
        AssetAssignmentCreateRequest request = AssetAssignmentCreateRequest.builder()
                .assetId(availableAsset.getId())
                .jobId(job.getId())
                .assignedWorkerId(worker.getId())
                .notes("Assigning for test job")
                .build();

        mockMvc.perform(post("/api/v1/asset-assignments/assign")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentId").exists())
                .andExpect(jsonPath("$.assetId").value(availableAsset.getId()))
                .andExpect(jsonPath("$.jobId").value(job.getId()))
                .andExpect(jsonPath("$.assignedWorkerId").value(worker.getId()))
                .andExpect(jsonPath("$.notes").value("Assigning for test job"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.assignedAt").exists());
    }

    @Test
    void shouldAssignAssetWithoutJobOrWorker() throws Exception {
        AssetAssignmentCreateRequest request = AssetAssignmentCreateRequest.builder()
                .assetId(availableAsset.getId())
                .build();

        mockMvc.perform(post("/api/v1/asset-assignments/assign")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetId").value(availableAsset.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn404WhenAssigningNonExistentAsset() throws Exception {
        AssetAssignmentCreateRequest request = AssetAssignmentCreateRequest.builder()
                .assetId(99999L)
                .build();

        mockMvc.perform(post("/api/v1/asset-assignments/assign")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAssigningAnotherCompanyAsset() throws Exception {
        AssetAssignmentCreateRequest request = AssetAssignmentCreateRequest.builder()
                .assetId(anotherCompanyAsset.getId())
                .build();

        mockMvc.perform(post("/api/v1/asset-assignments/assign")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenAssigningWithoutToken() throws Exception {
        AssetAssignmentCreateRequest request = AssetAssignmentCreateRequest.builder()
                .assetId(availableAsset.getId())
                .build();

        mockMvc.perform(post("/api/v1/asset-assignments/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForWorkerRoleOnAssign() throws Exception {
        // WORKER users have no company context; CompanyRoleAspect rejects with 403
        AssetAssignmentCreateRequest request = AssetAssignmentCreateRequest.builder()
                .assetId(availableAsset.getId())
                .build();

        mockMvc.perform(post("/api/v1/asset-assignments/assign")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= POST /api/v1/asset-assignments/return =============

    @Test
    void shouldReturnAssetSuccessfully() throws Exception {
        AssetAssignmentReturnRequest request = AssetAssignmentReturnRequest.builder()
                .assignmentId(existingAssignment.getId())
                .notes("Returning after use")
                .build();

        mockMvc.perform(post("/api/v1/asset-assignments/return")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentId").value(existingAssignment.getId()))
                .andExpect(jsonPath("$.returnedAt").exists())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldReturn404WhenReturningNonExistentAssignment() throws Exception {
        AssetAssignmentReturnRequest request = AssetAssignmentReturnRequest.builder()
                .assignmentId(99999L)
                .build();

        mockMvc.perform(post("/api/v1/asset-assignments/return")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/asset-assignments/asset/{assetId}/history =============

    @Test
    void shouldGetAssignmentHistorySuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/asset-assignments/asset/" + existingAssignment.getAsset().getId() + "/history")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].assetId").value(existingAssignment.getAsset().getId()));
    }

    @Test
    void shouldReturnEmptyHistoryForAssetWithNoAssignments() throws Exception {
        mockMvc.perform(get("/api/v1/asset-assignments/asset/" + availableAsset.getId() + "/history")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn404WhenGettingHistoryForNonExistentAsset() throws Exception {
        mockMvc.perform(get("/api/v1/asset-assignments/asset/99999/history")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/asset-assignments/job/{jobId} =============

    @Test
    void shouldGetJobAssignmentsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/asset-assignments/job/" + job.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .param("onlyActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void shouldGetAllJobAssignmentsIncludingReturned() throws Exception {
        mockMvc.perform(get("/api/v1/asset-assignments/job/" + job.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .param("onlyActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturn404WhenGettingAssignmentsForNonExistentJob() throws Exception {
        mockMvc.perform(get("/api/v1/asset-assignments/job/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenGettingAssignmentsForAnotherCompanyJob() throws Exception {
        // anotherCompany doesn't own `job`
        mockMvc.perform(get("/api/v1/asset-assignments/job/" + job.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401ForInvalidTokenOnAssignmentHistory() throws Exception {
        mockMvc.perform(get("/api/v1/asset-assignments/asset/" + availableAsset.getId() + "/history")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized());
    }
}
