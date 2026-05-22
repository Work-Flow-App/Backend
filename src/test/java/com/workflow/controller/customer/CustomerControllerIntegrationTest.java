package com.workflow.controller.customer;

import com.workflow.AbstractControllerIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.Role;
import com.workflow.dto.customer.CustomerAddressDto;
import com.workflow.dto.customer.CustomerCreateRequest;
import com.workflow.dto.customer.CustomerUpdateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.customer.Customer;
import com.workflow.entity.auth.User;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.customer.CustomerRepository;
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

class CustomerControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Company company;
    private Company anotherCompany;
    private Customer existingCustomer;
    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        User companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("customerowner")
                .password(passwordEncoder.encode("password")).email("customerowner@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anothercustomerowner")
                .password(passwordEncoder.encode("password")).email("anothercustomer@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("customerworker")
                .password(passwordEncoder.encode("password")).email("customerworker@test.com")
                .role(Role.WORKER).enabled(true).build());

        company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("customerowner@test.com").archived(false).build());

        anotherCompany = companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("anothercustomer@test.com").archived(false).build());

        createCompanyMember(company, companyUser, CompanyRole.COMPANY_ADMIN);
        createCompanyMember(anotherCompany, anotherUser, CompanyRole.COMPANY_ADMIN);

        existingCustomer = customerRepository.save(Customer.builder()
                .name("Existing Customer")
                .company(company)
                .email("existing@customer.com")
                .telephone("1234567890")
                .mobile("0987654321")
                .archived(false)
                .build());

        companyToken = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken = jwtService.generateToken(workerUser);
    }

    // ============= POST /api/v1/customers =============

    @Test
    void shouldCreateCustomerSuccessfully() throws Exception {
        CustomerCreateRequest request = CustomerCreateRequest.builder()
                .name("New Customer")
                .email("new@customer.com")
                .telephone("5551234567")
                .mobile("5559876543")
                .build();

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Customer"))
                .andExpect(jsonPath("$.email").value("new@customer.com"))
                .andExpect(jsonPath("$.telephone").value("5551234567"))
                .andExpect(jsonPath("$.mobile").value("5559876543"))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldCreateCustomerWithAddress() throws Exception {
        CustomerAddressDto address = new CustomerAddressDto();
        address.setHouseNumber("100");
        address.setStreet("Oak Street");
        address.setCity("London");
        address.setPostalCode("EC1A 1BB");
        address.setCountry("UK");

        CustomerCreateRequest request = CustomerCreateRequest.builder()
                .name("Customer With Address")
                .address(address)
                .build();

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Customer With Address"));
    }

    @Test
    void shouldReturn400WhenCustomerNameIsBlank() throws Exception {
        CustomerCreateRequest request = CustomerCreateRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenCustomerNameTooShort() throws Exception {
        CustomerCreateRequest request = CustomerCreateRequest.builder()
                .name("X")
                .build();

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403WhenCreatingCustomerWithoutToken() throws Exception {
        CustomerCreateRequest request = CustomerCreateRequest.builder().name("Customer").build();

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401ForInvalidTokenOnCreate() throws Exception {
        CustomerCreateRequest request = CustomerCreateRequest.builder().name("Customer").build();

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer bad.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForWorkerRoleOnCreate() throws Exception {
        // WORKER users have no company context set, so CompanyRoleAspect rejects with 403
        CustomerCreateRequest request = CustomerCreateRequest.builder().name("Customer Name").build();

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/customers =============

    @Test
    void shouldGetAllCustomersSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(existingCustomer.getId()))
                .andExpect(jsonPath("$[0].name").value("Existing Customer"));
    }

    @Test
    void shouldReturnEmptyListWhenNoCustomersForCompany() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn403ForMissingAuthOnGetAll() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/customers/{id} =============

    @Test
    void shouldGetCustomerByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + existingCustomer.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingCustomer.getId()))
                .andExpect(jsonPath("$.name").value("Existing Customer"))
                .andExpect(jsonPath("$.email").value("existing@customer.com"))
                .andExpect(jsonPath("$.telephone").value("1234567890"));
    }

    @Test
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/customers/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAccessingAnotherCompanyCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + existingCustomer.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= PUT /api/v1/customers/{id} =============

    @Test
    void shouldUpdateCustomerSuccessfully() throws Exception {
        CustomerUpdateRequest request = CustomerUpdateRequest.builder()
                .name("Updated Customer Name")
                .email("updated@customer.com")
                .telephone("9998887777")
                .mobile("6665554444")
                .archived(false)
                .build();

        mockMvc.perform(put("/api/v1/customers/" + existingCustomer.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingCustomer.getId()))
                .andExpect(jsonPath("$.name").value("Updated Customer Name"))
                .andExpect(jsonPath("$.email").value("updated@customer.com"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentCustomer() throws Exception {
        CustomerUpdateRequest request = CustomerUpdateRequest.builder()
                .name("Some Customer")
                .archived(false)
                .build();

        mockMvc.perform(put("/api/v1/customers/99999")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingAnotherCompanyCustomer() throws Exception {
        CustomerUpdateRequest request = CustomerUpdateRequest.builder()
                .name("Updated Name")
                .archived(false)
                .build();

        mockMvc.perform(put("/api/v1/customers/" + existingCustomer.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenUpdatingWithBlankName() throws Exception {
        CustomerUpdateRequest request = CustomerUpdateRequest.builder()
                .name("")
                .archived(false)
                .build();

        mockMvc.perform(put("/api/v1/customers/" + existingCustomer.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ============= DELETE /api/v1/customers/{id} =============

    @Test
    void shouldDeleteCustomerSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + existingCustomer.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentCustomer() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingAnotherCompanyCustomer() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + existingCustomer.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenDeletingWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + existingCustomer.getId()))
                .andExpect(status().isForbidden());
    }
}
