package com.workflow.controller.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobFieldType;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.dto.job.JobCreateRequest;
import com.workflow.dto.job.JobUpdateRequest;
import com.workflow.entity.*;
import com.workflow.repository.*;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobTemplateRepository jobTemplateRepository;

    @Autowired
    private JobTemplateFieldRepository jobTemplateFieldRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobFieldValueRepository jobFieldValueRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User companyUser;
    private User anotherCompanyUser;
    private User workerUser;
    private Company company;
    private Company anotherCompany;
    private JobTemplate template;
    private JobTemplateField field1;
    private JobTemplateField field2;
    private Client client;
    private Customer customer;
    private Worker worker;
    private Job job;
    private String companyUserToken;
    private String anotherCompanyUserToken;
    private String workerUserToken;

    @BeforeEach
    void setUp() {
        // Clear database
        jobFieldValueRepository.deleteAll();
        jobRepository.deleteAll();
        jobTemplateFieldRepository.deleteAll();
        jobTemplateRepository.deleteAll();
        workerRepository.deleteAll();
        clientRepository.deleteAll();
        customerRepository.deleteAll();
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

        // Create another company user
        anotherCompanyUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("anotherowner")
                .password(passwordEncoder.encode("password123"))
                .email("another@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();
        anotherCompanyUser = userRepository.save(anotherCompanyUser);

        // Create worker user
        workerUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("workeruser")
                .password(passwordEncoder.encode("password123"))
                .email("worker@example.com")
                .role(Role.WORKER)
                .enabled(true)
                .build();
        workerUser = userRepository.save(workerUser);

        // Create companies
        company = Company.builder()
                .name("Test Company")
                .user(companyUser)
                .email("company@example.com")
                .archived(false)
                .build();
        company = companyRepository.save(company);

        anotherCompany = Company.builder()
                .name("Another Company")
                .user(anotherCompanyUser)
                .email("another@example.com")
                .archived(false)
                .build();
        anotherCompany = companyRepository.save(anotherCompany);

        // Create client
        client = Client.builder()
                .name("Test Client")
                .company(company)
                .email("client@example.com")
                .archived(false)
                .build();
        client = clientRepository.save(client);

        // Create customer
        customer = Customer.builder()
                .name("Test Customer")
                .company(company)
                .email("customer@example.com")
                .build();
        customer = customerRepository.save(customer);

        // Create worker
        worker = Worker.builder()
                .name("Test Worker")
                .company(company)
                .user(workerUser)
                .email("worker@example.com")
                .archived(false)
                .build();
        worker = workerRepository.save(worker);

        // Create job template
        template = JobTemplate.builder()
                .name("Test Template")
                .description("Test Description")
                .company(company)
                .build();
        template = jobTemplateRepository.save(template);

        // Create template fields
        field1 = JobTemplateField.builder()
                .template(template)
                .name("customerName")
                .label("Customer Name")
                .jobFieldType(JobFieldType.TEXT)
                .required(true)
                .orderIndex(1)
                .build();
        field1 = jobTemplateFieldRepository.save(field1);

        field2 = JobTemplateField.builder()
                .template(template)
                .name("priority")
                .label("Priority Level")
                .jobFieldType(JobFieldType.DROPDOWN)
                .required(false)
                .options("[\"Low\", \"Medium\", \"High\"]")
                .orderIndex(2)
                .build();
        field2 = jobTemplateFieldRepository.save(field2);

        // Create job
        job = Job.builder()
                .template(template)
                .company(company)
                .client(client)
                .customer(customer)
                .assignedWorker(worker)
                .status(JobStatus.NEW)
                .archived(false)
                .build();
        job = jobRepository.save(job);

        // Create field values
        JobFieldValue fieldValue1 = JobFieldValue.builder()
                .job(job)
                .field(field1)
                .stringValue("John Doe")
                .build();
        jobFieldValueRepository.save(fieldValue1);

        JobFieldValue fieldValue2 = JobFieldValue.builder()
                .job(job)
                .field(field2)
                .stringValue("High")
                .build();
        jobFieldValueRepository.save(fieldValue2);

        // Generate JWT tokens
        companyUserToken = jwtService.generateToken(companyUser);
        anotherCompanyUserToken = jwtService.generateToken(anotherCompanyUser);
        workerUserToken = jwtService.generateToken(workerUser);
    }

    // ============= POST /api/v1/jobs Tests =============

    @Test
    void shouldCreateJobSuccessfully() throws Exception {
        Map<Long, Object> fieldValues = new HashMap<>();
        fieldValues.put(field1.getId(), "Jane Smith");
        fieldValues.put(field2.getId(), "Low");

        JobCreateRequest request = JobCreateRequest.builder()
                .templateId(template.getId())
                .clientId(client.getId())
                .customerId(customer.getId())
                .assignedWorkerId(worker.getId())
                .status(JobStatus.NEW)
                .fieldValues(fieldValues)
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.templateId").value(template.getId()))
                .andExpect(jsonPath("$.clientId").value(client.getId()))
                .andExpect(jsonPath("$.assignedWorkerId").value(worker.getId()))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.fieldValues").exists());
    }

    @Test
    void shouldCreateJobWithoutClientAndWorker() throws Exception {
        Map<Long, Object> fieldValues = new HashMap<>();
        fieldValues.put(field1.getId(), "No Client Job");

        JobCreateRequest request = JobCreateRequest.builder()
                .templateId(template.getId())
                .clientId(null)
                .customerId(customer.getId())
                .assignedWorkerId(null)
                .status(JobStatus.NEW)
                .fieldValues(fieldValues)
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").isEmpty())
                .andExpect(jsonPath("$.assignedWorkerId").isEmpty());
    }

    @Test
    void shouldReturn404WhenTemplateNotFound() throws Exception {
        JobCreateRequest request = JobCreateRequest.builder()
                .templateId(99999L)
                .customerId(customer.getId())
                .fieldValues(new HashMap<>())
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenClientNotFound() throws Exception {
        JobCreateRequest request = JobCreateRequest.builder()
                .templateId(template.getId())
                .clientId(99999L)
                .customerId(customer.getId())
                .fieldValues(new HashMap<>())
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenWorkerNotFound() throws Exception {
        JobCreateRequest request = JobCreateRequest.builder()
                .templateId(template.getId())
                .customerId(customer.getId())
                .assignedWorkerId(99999L)
                .fieldValues(new HashMap<>())
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenCreatingJobWithoutToken() throws Exception {
        JobCreateRequest request = JobCreateRequest.builder()
                .templateId(template.getId())
                .fieldValues(new HashMap<>())
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= PUT /api/v1/jobs/{id} Tests =============

    @Test
    void shouldUpdateJobSuccessfully() throws Exception {
        Map<Long, Object> fieldValues = new HashMap<>();
        fieldValues.put(field1.getId(), "Updated Name");
        fieldValues.put(field2.getId(), "Medium");

        JobUpdateRequest request = JobUpdateRequest.builder()
                .clientId(client.getId())
                .assignedWorkerId(worker.getId())
                .status(JobStatus.IN_PROGRESS)
                .archived(false)
                .fieldValues(fieldValues)
                .build();

        mockMvc.perform(put("/api/v1/jobs/" + job.getId())
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.fieldValues").exists());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentJob() throws Exception {
        JobUpdateRequest request = JobUpdateRequest.builder()
                .status(JobStatus.COMPLETED)
                .archived(false)
                .fieldValues(new HashMap<>())
                .build();

        mockMvc.perform(put("/api/v1/jobs/99999")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingAnotherCompanyJob() throws Exception {
        JobUpdateRequest request = JobUpdateRequest.builder()
                .status(JobStatus.COMPLETED)
                .archived(false)
                .fieldValues(new HashMap<>())
                .build();

        mockMvc.perform(put("/api/v1/jobs/" + job.getId())
                        .header("Authorization", "Bearer " + anotherCompanyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/jobs/{id} Tests =============

    @Test
    void shouldGetJobByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/" + job.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId()))
                .andExpect(jsonPath("$.templateId").value(template.getId()))
                .andExpect(jsonPath("$.clientId").value(client.getId()))
                .andExpect(jsonPath("$.assignedWorkerId").value(worker.getId()))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.fieldValues").exists())
                .andExpect(jsonPath("$.fieldValues." + field1.getId() + ".value").value("John Doe"))
                .andExpect(jsonPath("$.fieldValues." + field2.getId() + ".value").value("High"));
    }

    @Test
    void shouldReturn404WhenJobNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/99999")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAccessingAnotherCompanyJob() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/" + job.getId())
                        .header("Authorization", "Bearer " + anotherCompanyUserToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/jobs Tests =============

    @Test
    void shouldGetAllJobsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(job.getId()))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    void shouldReturnEmptyListWhenNoJobsExist() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + anotherCompanyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ============= GET /api/v1/jobs/templates/{templateId} Tests =============

    @Test
    void shouldGetJobsByTemplateSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/templates/" + template.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(job.getId()))
                .andExpect(jsonPath("$[0].templateId").value(template.getId()));
    }

    @Test
    void shouldReturn404WhenTemplateNotFoundForGetJobsByTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/templates/99999")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnEmptyListWhenNoJobsForTemplate() throws Exception {
        // Create a new template without jobs
        JobTemplate emptyTemplate = JobTemplate.builder()
                .name("Empty Template")
                .company(company)
                .build();
        emptyTemplate = jobTemplateRepository.save(emptyTemplate);

        mockMvc.perform(get("/api/v1/jobs/templates/" + emptyTemplate.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ============= DELETE /api/v1/jobs/{id} Tests =============

    @Test
    void shouldDeleteJobSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/jobs/" + job.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentJob() throws Exception {
        mockMvc.perform(delete("/api/v1/jobs/99999")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingAnotherCompanyJob() throws Exception {
        mockMvc.perform(delete("/api/v1/jobs/" + job.getId())
                        .header("Authorization", "Bearer " + anotherCompanyUserToken))
                .andExpect(status().isNotFound());
    }

    // ============= Authorization Tests =============

    @Test
    void shouldReturn403ForWorkerRole() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + workerUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401ForInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isForbidden());
    }
}