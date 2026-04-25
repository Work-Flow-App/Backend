package com.workflow.controller.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.dto.company.CompanyAddressRequest;
import com.workflow.dto.company.CompanyProfileUpdateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.auth.User;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.auth.UserRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompanyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User companyUser;
    private User workerUser;
    private Company company;
    private String companyUserToken;
    private String workerUserToken;

    @BeforeEach
    void setUp() {
        // Clear database
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

        // Create company
        company = Company.builder()
                .name("Test Company")
                .user(companyUser)
                .email("company@example.com")
                .telephone("1234567890")
                .archived(false)
                .build();
        company = companyRepository.save(company);

        // Generate JWT tokens
        companyUserToken = jwtService.generateToken(companyUser);
        workerUserToken = jwtService.generateToken(workerUser);
    }

    // ============= GET /api/v1/companies/profile Tests =============

    @Test
    void shouldGetCompanyProfileSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/companies/profile")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(company.getId()))
                .andExpect(jsonPath("$.name").value("Test Company"))
                .andExpect(jsonPath("$.email").value("company@example.com"))
                .andExpect(jsonPath("$.telephone").value("1234567890"))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void shouldReturn403ForbiddenWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/companies/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForbiddenForWorkerRole() throws Exception {
        mockMvc.perform(get("/api/v1/companies/profile")
                        .header("Authorization", "Bearer " + workerUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenCompanyNotFound() throws Exception {
        // Create another company user without company
        User anotherUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("nocompany")
                .password(passwordEncoder.encode("password123"))
                .email("nocompany@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();
        anotherUser = userRepository.save(anotherUser);
        String token = jwtService.generateToken(anotherUser);

        mockMvc.perform(get("/api/v1/companies/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("No active company found")));
    }

    // ============= POST /api/v1/companies/profile Tests =============

    @Test
    void shouldUpdateCompanyProfileSuccessfully() throws Exception {
        CompanyProfileUpdateRequest request = new CompanyProfileUpdateRequest(
                "Updated Company Name",
                new CompanyAddressRequest("123 Main St", "Suite 100", null, "New York", "USA", "10001"),
                "9876543210",
                "9876543210",
                "1234567890",
                "updated@example.com",
                "contact@example.com",
                "ACC123",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/companies/profile")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Company Name"))
                .andExpect(jsonPath("$.address.addressLine1").value("123 Main St"))
                .andExpect(jsonPath("$.address.addressLine2").value("Suite 100"))
                .andExpect(jsonPath("$.address.town").value("New York"))
                .andExpect(jsonPath("$.address.country").value("USA"))
                .andExpect(jsonPath("$.address.postcode").value("10001"))
                .andExpect(jsonPath("$.telephone").value("9876543210"))
                .andExpect(jsonPath("$.mobile").value("9876543210"))
                .andExpect(jsonPath("$.fax").value("1234567890"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.contactEmail").value("contact@example.com"))
                .andExpect(jsonPath("$.contactNumber").value("ACC123"));
    }

    @Test
    void shouldReturn400BadRequestForInvalidEmail() throws Exception {
        CompanyProfileUpdateRequest request = new CompanyProfileUpdateRequest(
                "Test Company",
                null, null, null, null,
                "invalid-email",
                null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/companies/profile")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.email").value(containsString("Invalid email format")));
    }

    @Test
    void shouldReturn400BadRequestForInvalidContactEmail() throws Exception {
        CompanyProfileUpdateRequest request = new CompanyProfileUpdateRequest(
                "Test Company",
                null, null, null, null,
                "valid@example.com",
                "invalid-contact-email",
                null, null, null, null
        );

        mockMvc.perform(post("/api/v1/companies/profile")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.contactEmail").exists());
    }

    @Test
    void shouldReturn400BadRequestForMissingCompanyName() throws Exception {
        CompanyProfileUpdateRequest request = new CompanyProfileUpdateRequest(
                "",
                null, null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/companies/profile")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    void shouldReturn409ConflictForDuplicateCompanyName() throws Exception {
        // Create another company with different user
        User anotherUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("anothercompany")
                .password(passwordEncoder.encode("password123"))
                .email("another@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();
        anotherUser = userRepository.save(anotherUser);

        Company anotherCompany = Company.builder()
                .name("Another Company")
                .user(anotherUser)
                .email("another@example.com")
                .archived(false)
                .build();
        companyRepository.save(anotherCompany);

        // Try to update first company with the second company's name
        CompanyProfileUpdateRequest request = new CompanyProfileUpdateRequest(
                "Another Company",
                null, null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/companies/profile")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void shouldAllowUpdatingSameNameWithDifferentCase() throws Exception {
        CompanyProfileUpdateRequest request = new CompanyProfileUpdateRequest(
                "test company", // lowercase
                null, null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/companies/profile")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test company"));
    }

    @Test
    void shouldValidateFieldLengths() throws Exception {
        String longString = "a".repeat(300);

        CompanyProfileUpdateRequest request = new CompanyProfileUpdateRequest(
                longString, // Exceeds 150 characters
                null, null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/companies/profile")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    // ============= GET /api/v1/companies/dashboard Tests =============

    @Test
    void shouldGetDashboardSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/companies/dashboard")
                        .header("Authorization", "Bearer " + companyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(company.getId()))
                .andExpect(jsonPath("$.companyName").value("Test Company"))
                .andExpect(jsonPath("$.totalWorkers").value(0))
                .andExpect(jsonPath("$.totalClients").value(0))
                .andExpect(jsonPath("$.activeWorkers").value(0))
                .andExpect(jsonPath("$.archivedWorkers").value(0));
    }

    @Test
    void shouldReturn403ForbiddenForDashboardWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/companies/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForbiddenForDashboardWithWorkerRole() throws Exception {
        mockMvc.perform(get("/api/v1/companies/dashboard")
                        .header("Authorization", "Bearer " + workerUserToken))
                .andExpect(status().isForbidden());
    }

    // ============= Token Validation Tests =============

    @Test
    void shouldReturn401ForInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/companies/profile")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForMalformedAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/companies/profile")
                        .header("Authorization", "InvalidFormat " + companyUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldHandleNullFieldsInUpdate() throws Exception {
        CompanyProfileUpdateRequest request = new CompanyProfileUpdateRequest(
                "Only Name Updated",
                null, null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/companies/profile")
                        .header("Authorization", "Bearer " + companyUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Only Name Updated"))
                .andExpect(jsonPath("$.address").value(nullValue()));
    }
}