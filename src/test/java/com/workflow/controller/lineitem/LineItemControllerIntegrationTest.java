package com.workflow.controller.lineitem;

import com.workflow.AbstractControllerIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.Role;
import com.workflow.dto.estimate.LineItemCreateRequest;
import com.workflow.dto.estimate.LineItemUpdateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.financial.LineItem;
import com.workflow.entity.auth.User;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.financial.LineItemRepository;
import com.workflow.repository.auth.UserRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LineItemControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private LineItemRepository lineItemRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Company company;
    private Company anotherCompany;
    private LineItem existingLineItem;
    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        lineItemRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        User companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("lineitemowner")
                .password(passwordEncoder.encode("password")).email("lineitemowner@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anotherlineowner")
                .password(passwordEncoder.encode("password")).email("anotherline@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("lineitemworker")
                .password(passwordEncoder.encode("password")).email("lineitemworker@test.com")
                .role(Role.WORKER).enabled(true).build());

        company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("lineitemowner@test.com").archived(false).build());

        anotherCompany = companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("anotherline@test.com").archived(false).build());

        createCompanyMember(company, companyUser, CompanyRole.COMPANY_ADMIN);
        createCompanyMember(anotherCompany, anotherUser, CompanyRole.COMPANY_ADMIN);

        existingLineItem = lineItemRepository.save(LineItem.builder()
                .company(company)
                .productCode("PROD-001")
                .productDescription("Labour Hours")
                .additionalDetails("Additional notes")
                .unitPrice(new BigDecimal("50.00"))
                .quantity(new BigDecimal("2.0000"))
                .vatRate(new BigDecimal("20.00"))
                .netAmount(new BigDecimal("100.00"))
                .vatAmount(new BigDecimal("20.00"))
                .totalAmount(new BigDecimal("120.00"))
                .build());

        companyToken = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken = jwtService.generateToken(workerUser);
    }

    // ============= POST /api/v1/line-items =============

    @Test
    void shouldCreateLineItemSuccessfully() throws Exception {
        LineItemCreateRequest request = LineItemCreateRequest.builder()
                .productCode("NEW-001")
                .productDescription("New Service")
                .additionalDetails("Some details")
                .unitPrice(new BigDecimal("75.00"))
                .quantity(new BigDecimal("3"))
                .vatRate(new BigDecimal("20.00"))
                .build();

        mockMvc.perform(post("/api/v1/line-items")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.productCode").value("NEW-001"))
                .andExpect(jsonPath("$.productDescription").value("New Service"))
                .andExpect(jsonPath("$.unitPrice").value(75.00))
                .andExpect(jsonPath("$.quantity").value(3.0))
                .andExpect(jsonPath("$.netAmount").value(225.00))
                .andExpect(jsonPath("$.vatAmount").value(45.00))
                .andExpect(jsonPath("$.totalAmount").value(270.00));
    }

    @Test
    void shouldReturn400WhenProductCodeIsBlank() throws Exception {
        LineItemCreateRequest request = LineItemCreateRequest.builder()
                .productCode("")
                .productDescription("Service")
                .unitPrice(new BigDecimal("50.00"))
                .quantity(new BigDecimal("1"))
                .vatRate(new BigDecimal("20.00"))
                .build();

        mockMvc.perform(post("/api/v1/line-items")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenProductDescriptionIsBlank() throws Exception {
        LineItemCreateRequest request = LineItemCreateRequest.builder()
                .productCode("P001")
                .productDescription("")
                .unitPrice(new BigDecimal("50.00"))
                .quantity(new BigDecimal("1"))
                .vatRate(new BigDecimal("20.00"))
                .build();

        mockMvc.perform(post("/api/v1/line-items")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenQuantityIsZero() throws Exception {
        LineItemCreateRequest request = LineItemCreateRequest.builder()
                .productCode("P001")
                .productDescription("Service")
                .unitPrice(new BigDecimal("50.00"))
                .quantity(new BigDecimal("0"))
                .vatRate(new BigDecimal("20.00"))
                .build();

        mockMvc.perform(post("/api/v1/line-items")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403WhenCreatingLineItemWithoutToken() throws Exception {
        LineItemCreateRequest request = LineItemCreateRequest.builder()
                .productCode("P001").productDescription("Service")
                .unitPrice(new BigDecimal("50"))
                .quantity(new BigDecimal("1")).vatRate(new BigDecimal("20")).build();

        mockMvc.perform(post("/api/v1/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForWorkerRoleOnCreate() throws Exception {
        LineItemCreateRequest request = LineItemCreateRequest.builder()
                .productCode("P001").productDescription("Service")
                .unitPrice(new BigDecimal("50"))
                .quantity(new BigDecimal("1")).vatRate(new BigDecimal("20")).build();

        mockMvc.perform(post("/api/v1/line-items")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/line-items =============

    @Test
    void shouldGetAllLineItemsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/line-items")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(existingLineItem.getId()))
                .andExpect(jsonPath("$[0].productCode").value("PROD-001"))
                .andExpect(jsonPath("$[0].productDescription").value("Labour Hours"));
    }

    @Test
    void shouldReturnEmptyListForAnotherCompany() throws Exception {
        mockMvc.perform(get("/api/v1/line-items")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn403ForMissingAuthOnGetAll() throws Exception {
        mockMvc.perform(get("/api/v1/line-items"))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/line-items/{id} =============

    @Test
    void shouldGetLineItemByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingLineItem.getId()))
                .andExpect(jsonPath("$.productCode").value("PROD-001"))
                .andExpect(jsonPath("$.unitPrice").value(50.00))
                .andExpect(jsonPath("$.netAmount").value(100.00))
                .andExpect(jsonPath("$.totalAmount").value(120.00));
    }

    @Test
    void shouldReturn404WhenLineItemNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/line-items/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAccessingAnotherCompanyLineItem() throws Exception {
        mockMvc.perform(get("/api/v1/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= PUT /api/v1/line-items/{id} =============

    @Test
    void shouldUpdateLineItemSuccessfully() throws Exception {
        LineItemUpdateRequest request = LineItemUpdateRequest.builder()
                .productCode("UPDATED-001")
                .productDescription("Updated Service")
                .unitPrice(new BigDecimal("60.00"))
                .quantity(new BigDecimal("4"))
                .vatRate(new BigDecimal("20.00"))
                .build();

        mockMvc.perform(put("/api/v1/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingLineItem.getId()))
                .andExpect(jsonPath("$.productCode").value("UPDATED-001"))
                .andExpect(jsonPath("$.productDescription").value("Updated Service"))
                .andExpect(jsonPath("$.unitPrice").value(60.00))
                .andExpect(jsonPath("$.netAmount").value(240.00))
                .andExpect(jsonPath("$.totalAmount").value(288.00));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentLineItem() throws Exception {
        LineItemUpdateRequest request = LineItemUpdateRequest.builder()
                .productCode("P001").build();

        mockMvc.perform(put("/api/v1/line-items/99999")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingAnotherCompanyLineItem() throws Exception {
        LineItemUpdateRequest request = LineItemUpdateRequest.builder()
                .productCode("P001").build();

        mockMvc.perform(put("/api/v1/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= DELETE /api/v1/line-items/{id} =============

    @Test
    void shouldDeleteLineItemSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentLineItem() throws Exception {
        mockMvc.perform(delete("/api/v1/line-items/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingAnotherCompanyLineItem() throws Exception {
        mockMvc.perform(delete("/api/v1/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401ForInvalidTokenOnGetAll() throws Exception {
        mockMvc.perform(get("/api/v1/line-items")
                        .header("Authorization", "Bearer bad.token.value"))
                .andExpect(status().isUnauthorized());
    }
}
