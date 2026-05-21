package com.workflow.controller.jobtemplate;

import com.workflow.AbstractControllerIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobFieldType;
import com.workflow.dto.jobtemplate.JobTemplateCreateRequest;
import com.workflow.dto.jobtemplate.JobTemplateFieldCreateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.job.JobTemplateField;
import com.workflow.entity.auth.User;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.job.JobTemplateFieldRepository;
import com.workflow.repository.job.JobTemplateRepository;
import com.workflow.repository.auth.UserRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JobTemplateControllerIntegrationTest extends AbstractControllerIntegrationTest {

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
    private String companyUserToken;
    private String anotherCompanyUserToken;
    private String workerUserToken;

    @BeforeEach
    void setUp() {
        // Clear database
        jobTemplateFieldRepository.deleteAll();
        jobTemplateRepository.deleteAll();
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

        createCompanyMember(company, companyUser, CompanyRole.COMPANY_ADMIN);
        createCompanyMember(anotherCompany, anotherCompanyUser, CompanyRole.COMPANY_ADMIN);

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

        // Generate JWT tokens
        companyUserToken = jwtService.generateToken(companyUser);
        anotherCompanyUserToken = jwtService.generateToken(anotherCompanyUser);
        workerUserToken = jwtService.generateToken(workerUser);
    }

    // ============= POST /api/v1/job-templates Tests =============

    @Test
    void shouldCreateTemplateSuccessfully() throws Exception {
        JobTemplateCreateRequest request = JobTemplateCreateRequest.builder()
                .name("New Template")
                .description("New Description")
                .build();

        mockMvc.perform(post("/api/v1/job-templates")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Template"))
                .andExpect(jsonPath("$.description").value("New Description"))
                .andExpect(jsonPath("$.companyId").value(company.getId()));
    }

    @Test
    void shouldReturn403WhenCreatingTemplateWithoutToken() throws Exception {
        JobTemplateCreateRequest request = JobTemplateCreateRequest.builder()
                .name("New Template")
                .build();

        mockMvc.perform(post("/api/v1/job-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn409WhenCreatingDuplicateTemplate() throws Exception {
        JobTemplateCreateRequest request = JobTemplateCreateRequest.builder()
                .name("Test Template")  // Duplicate name
                .description("Another Description")
                .build();

        mockMvc.perform(post("/api/v1/job-templates")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ============= GET /api/v1/job-templates Tests =============

    @Test
    void shouldGetAllTemplatesSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(template.getId()))
                .andExpect(jsonPath("$[0].name").value("Test Template"))
                .andExpect(jsonPath("$[0].description").value("Test Description"));
    }

    @Test
    void shouldReturnEmptyListWhenNoTemplatesExist() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates")
                        .header("Authorization", "Bearer " + anotherCompanyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ============= GET /api/v1/job-templates/{id} Tests =============

    @Test
    void shouldGetTemplateByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates/" + template.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(template.getId()))
                .andExpect(jsonPath("$.name").value("Test Template"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.companyId").value(company.getId()));
    }

    @Test
    void shouldReturn404WhenTemplateNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates/99999")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAccessingAnotherCompanyTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates/" + template.getId())
                        .header("Authorization", "Bearer " + anotherCompanyUserToken))
                .andExpect(status().isNotFound());
    }

    // ============= PUT /api/v1/job-templates/{id} Tests =============

    @Test
    void shouldUpdateTemplateSuccessfully() throws Exception {
        JobTemplateCreateRequest request = JobTemplateCreateRequest.builder()
                .name("Updated Template")
                .description("Updated Description")
                .build();

        mockMvc.perform(put("/api/v1/job-templates/" + template.getId())
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(template.getId()))
                .andExpect(jsonPath("$.name").value("Updated Template"))
                .andExpect(jsonPath("$.description").value("Updated Description"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentTemplate() throws Exception {
        JobTemplateCreateRequest request = JobTemplateCreateRequest.builder()
                .name("Updated Template")
                .build();

        mockMvc.perform(put("/api/v1/job-templates/99999")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= DELETE /api/v1/job-templates/{id} Tests =============

    @Test
    void shouldDeleteTemplateSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/job-templates/" + template.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/job-templates/" + template.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentTemplate() throws Exception {
        mockMvc.perform(delete("/api/v1/job-templates/99999")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNotFound());
    }

    // ============= POST /api/v1/job-templates/fields Tests =============

    @Test
    void shouldCreateTemplateFieldSuccessfully() throws Exception {
        JobTemplateFieldCreateRequest request = JobTemplateFieldCreateRequest.builder()
                .templateId(template.getId())
                .name("newField")
                .label("New Field")
                .jobFieldType(JobFieldType.NUMBER)
                .required(true)
                .orderIndex(3)
                .build();

        mockMvc.perform(post("/api/v1/job-templates/fields")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("newField"))
                .andExpect(jsonPath("$.label").value("New Field"))
                .andExpect(jsonPath("$.jobFieldType").value("NUMBER"))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.orderIndex").value(3));
    }

    // ============= GET /api/v1/job-templates/{id}/fields Tests =============

    @Test
    void shouldGetTemplateFieldsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates/" + template.getId() + "/fields")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("customerName"))
                .andExpect(jsonPath("$[1].name").value("priority"));
    }

    // ============= GET /api/v1/job-templates/fields/{fieldId} Tests =============

    @Test
    void shouldGetTemplateFieldByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates/fields/" + field1.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(field1.getId()))
                .andExpect(jsonPath("$.name").value("customerName"))
                .andExpect(jsonPath("$.label").value("Customer Name"))
                .andExpect(jsonPath("$.jobFieldType").value("TEXT"))
                .andExpect(jsonPath("$.required").value(true));
    }

    @Test
    void shouldReturn404WhenFieldNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates/fields/99999")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNotFound());
    }

    // ============= PUT /api/v1/job-templates/fields/{fieldId} Tests =============

    @Test
    void shouldUpdateTemplateFieldSuccessfully() throws Exception {
        JobTemplateFieldCreateRequest request = JobTemplateFieldCreateRequest.builder()
                .templateId(template.getId())
                .name("updatedField")
                .label("Updated Field")
                .jobFieldType(JobFieldType.TEXT)
                .required(false)
                .orderIndex(1)
                .build();

        mockMvc.perform(put("/api/v1/job-templates/fields/" + field1.getId())
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updatedField"))
                .andExpect(jsonPath("$.label").value("Updated Field"))
                .andExpect(jsonPath("$.required").value(false));
    }

    // ============= DELETE /api/v1/job-templates/fields/{fieldId} Tests =============

    @Test
    void shouldDeleteTemplateFieldSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/job-templates/fields/" + field1.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/job-templates/fields/" + field1.getId())
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/job-templates/{id}/with-fields Tests =============

    @Test
    void shouldGetTemplateWithFieldsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates/" + template.getId() + "/with-fields")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template.id").value(template.getId()))
                .andExpect(jsonPath("$.template.name").value("Test Template"))
                .andExpect(jsonPath("$.fields", hasSize(2)))
                .andExpect(jsonPath("$.fields[0].name").value("customerName"))
                .andExpect(jsonPath("$.fields[1].name").value("priority"));
    }

    @Test
    void shouldReturn404WhenGettingNonExistentTemplateWithFields() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates/99999/with-fields")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isNotFound());
    }

    // ============= Authorization Tests =============

    @Test
    void shouldReturn403ForWorkerRole() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates")
                        .header("Authorization", "Bearer " + workerUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401ForInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/job-templates")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }
}