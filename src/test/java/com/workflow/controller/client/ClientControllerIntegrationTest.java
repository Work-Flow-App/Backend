package com.workflow.controller.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.dto.client.ClientCreateRequest;
import com.workflow.dto.client.ClientUpdateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.customer.Client;
import com.workflow.entity.auth.User;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.customer.ClientRepository;
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
class ClientControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Company company;
    private Company anotherCompany;
    private Client existingClient;
    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        clientRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        User companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("clientowner")
                .password(passwordEncoder.encode("password")).email("clientowner@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anotherclientowner")
                .password(passwordEncoder.encode("password")).email("anotherclient@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("clientworker")
                .password(passwordEncoder.encode("password")).email("clientworker@test.com")
                .role(Role.WORKER).enabled(true).build());

        company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("clientowner@test.com").archived(false).build());

        anotherCompany = companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("anotherclient@test.com").archived(false).build());

        existingClient = clientRepository.save(Client.builder()
                .name("Existing Client")
                .company(company)
                .email("existing@client.com")
                .telephone("1234567890")
                .mobile("0987654321")
                .address("123 Main St")
                .archived(false)
                .build());

        companyToken = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken = jwtService.generateToken(workerUser);
    }

    // ============= POST /api/v1/clients =============

    @Test
    void shouldCreateClientSuccessfully() throws Exception {
        ClientCreateRequest request = ClientCreateRequest.builder()
                .name("New Client")
                .email("new@client.com")
                .telephone("5551234567")
                .mobile("5559876543")
                .address("456 Oak Ave")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Client"))
                .andExpect(jsonPath("$.email").value("new@client.com"))
                .andExpect(jsonPath("$.telephone").value("5551234567"))
                .andExpect(jsonPath("$.mobile").value("5559876543"))
                .andExpect(jsonPath("$.address").value("456 Oak Ave"))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn400WhenClientNameIsBlank() throws Exception {
        ClientCreateRequest request = ClientCreateRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenClientNameTooShort() throws Exception {
        ClientCreateRequest request = ClientCreateRequest.builder()
                .name("X")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403WhenCreatingClientWithoutToken() throws Exception {
        ClientCreateRequest request = ClientCreateRequest.builder().name("Client").build();

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401ForInvalidTokenOnCreate() throws Exception {
        ClientCreateRequest request = ClientCreateRequest.builder().name("Client").build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForWorkerRoleOnCreate() throws Exception {
        ClientCreateRequest request = ClientCreateRequest.builder().name("Client").build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/clients =============

    @Test
    void shouldGetAllClientsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(existingClient.getId()))
                .andExpect(jsonPath("$[0].name").value("Existing Client"));
    }

    @Test
    void shouldReturnEmptyListWhenNoClientsForCompany() throws Exception {
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn403ForMissingAuthOnGetAll() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isForbidden());
    }

    // ============= GET /api/v1/clients/{id} =============

    @Test
    void shouldGetClientByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + existingClient.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingClient.getId()))
                .andExpect(jsonPath("$.name").value("Existing Client"))
                .andExpect(jsonPath("$.email").value("existing@client.com"))
                .andExpect(jsonPath("$.telephone").value("1234567890"))
                .andExpect(jsonPath("$.mobile").value("0987654321"));
    }

    @Test
    void shouldReturn404WhenClientNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/clients/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAccessingAnotherCompanyClient() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + existingClient.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= PUT /api/v1/clients/{id} =============

    @Test
    void shouldUpdateClientSuccessfully() throws Exception {
        ClientUpdateRequest request = ClientUpdateRequest.builder()
                .name("Updated Client Name")
                .email("updated@client.com")
                .telephone("9998887777")
                .mobile("6665554444")
                .address("789 Pine Rd")
                .archived(false)
                .build();

        mockMvc.perform(put("/api/v1/clients/" + existingClient.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingClient.getId()))
                .andExpect(jsonPath("$.name").value("Updated Client Name"))
                .andExpect(jsonPath("$.email").value("updated@client.com"))
                .andExpect(jsonPath("$.telephone").value("9998887777"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentClient() throws Exception {
        ClientUpdateRequest request = ClientUpdateRequest.builder()
                .name("Some Client")
                .archived(false)
                .build();

        mockMvc.perform(put("/api/v1/clients/99999")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingAnotherCompanyClient() throws Exception {
        ClientUpdateRequest request = ClientUpdateRequest.builder()
                .name("Updated Name")
                .archived(false)
                .build();

        mockMvc.perform(put("/api/v1/clients/" + existingClient.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenUpdatingWithBlankName() throws Exception {
        ClientUpdateRequest request = ClientUpdateRequest.builder()
                .name("")
                .archived(false)
                .build();

        mockMvc.perform(put("/api/v1/clients/" + existingClient.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ============= DELETE /api/v1/clients/{id} =============

    @Test
    void shouldDeleteClientSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/" + existingClient.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentClient() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingAnotherCompanyClient() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/" + existingClient.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenDeletingWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/" + existingClient.getId()))
                .andExpect(status().isForbidden());
    }
}
