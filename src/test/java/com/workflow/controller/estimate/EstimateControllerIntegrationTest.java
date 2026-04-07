package com.workflow.controller.estimate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CoreOrSub;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.dto.estimate.EstimateUpdateRequest;
import com.workflow.dto.estimate.LineItemCreateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.customer.Customer;
import com.workflow.entity.financial.Estimate;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.financial.LineItem;
import com.workflow.entity.auth.User;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.customer.CustomerRepository;
import com.workflow.repository.financial.EstimateRepository;
import com.workflow.repository.financial.LineItemRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.job.JobTemplateRepository;
import com.workflow.service.auth.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EstimateControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    @PersistenceContext private EntityManager entityManager;

    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobTemplateRepository jobTemplateRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private EstimateRepository estimateRepository;
    @Autowired private LineItemRepository lineItemRepository;

    private Company company;
    private Company anotherCompany;
    private Job job;
    private Estimate estimate;
    private LineItem existingLineItem;

    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        lineItemRepository.deleteAll();
        estimateRepository.deleteAll();
        jobRepository.deleteAll();
        jobTemplateRepository.deleteAll();
        customerRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        User companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("companyowner")
                .password(passwordEncoder.encode("password")).email("company@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anotherown")
                .password(passwordEncoder.encode("password")).email("another@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("workeruser")
                .password(passwordEncoder.encode("password")).email("worker@test.com")
                .role(Role.WORKER).enabled(true).build());

        company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("company@test.com").archived(false).build());

        anotherCompany = companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("another@test.com").archived(false).build());

        JobTemplate template = jobTemplateRepository.save(JobTemplate.builder()
                .name("Default Template").company(company).build());

        Customer customer = customerRepository.save(Customer.builder()
                .name("Test Customer").company(company).email("customer@test.com").build());

        job = jobRepository.save(Job.builder()
                .template(template).company(company).customer(customer)
                .status(JobStatus.NEW).archived(false).build());

        // A standalone line item (not yet linked to any estimate)
        existingLineItem = lineItemRepository.save(LineItem.builder()
                .company(company).productCode("P-EXISTING").productDescription("Existing Item")
                .unitPrice(new BigDecimal("100.00")).coreOrSub(CoreOrSub.CORE)
                .quantity(new BigDecimal("1.0000")).vatRate(new BigDecimal("0.1900"))
                .netAmount(new BigDecimal("100.00")).vatAmount(new BigDecimal("19.00"))
                .totalAmount(new BigDecimal("119.00")).build());

        estimate = estimateRepository.save(Estimate.builder()
                .job(job).company(company).notes("Existing estimate").build());

        companyToken       = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken        = jwtService.generateToken(workerUser);
    }

    // ============= GET /api/v1/estimates/{id} =============

    @Test
    void shouldGetEstimateByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/" + estimate.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(estimate.getId()))
                .andExpect(jsonPath("$.notes").value("Existing estimate"))
                .andExpect(jsonPath("$.lineItems", hasSize(0)));
    }

    @Test
    void shouldReturn404WhenEstimateNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAccessingAnotherCompanyEstimate() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/" + estimate.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/estimates/job/{jobId} =============

    @Test
    void shouldGetEstimateByJobId() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/job/" + job.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(job.getId()));
    }

    // ============= PUT /api/v1/estimates/{id} =============

    @Test
    void shouldUpdateEstimateNotes() throws Exception {
        mockMvc.perform(put("/api/v1/estimates/" + estimate.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                EstimateUpdateRequest.builder().notes("Updated notes").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Updated notes"));
    }

    // ============= DELETE /api/v1/estimates/{id} =============

    @Test
    void shouldDeleteEstimateSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/estimates/" + estimate.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentEstimate() throws Exception {
        mockMvc.perform(delete("/api/v1/estimates/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    // ============= POST /{estimateId}/line-items — create new + link =============

    @Test
    void shouldCreateNewLineItemAndLinkToEstimate() throws Exception {
        // net = 50 × 2 = 100, vat = 100 × 19% = 19, total = 119
        LineItemCreateRequest request = LineItemCreateRequest.builder()
                .productCode("P001").productDescription("Labour")
                .unitPrice(new BigDecimal("50.00")).coreOrSub(CoreOrSub.CORE)
                .quantity(new BigDecimal("2")).vatRate(new BigDecimal("19.00")).build();

        mockMvc.perform(post("/api/v1/estimates/" + estimate.getId() + "/line-items")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lineItems", hasSize(1)))
                .andExpect(jsonPath("$.lineItems[0].productCode").value("P001"))
                .andExpect(jsonPath("$.lineItems[0].netAmount").value(100.00))
                .andExpect(jsonPath("$.lineItems[0].vatAmount").value(19.00))
                .andExpect(jsonPath("$.lineItems[0].totalAmount").value(119.00))
                .andExpect(jsonPath("$.totalNet").value(100.00))
                .andExpect(jsonPath("$.grandTotal").value(119.00));
    }

    @Test
    void shouldReturn400WhenLineItemMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/estimates/" + estimate.getId() + "/line-items")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenCreatingLineItemForAnotherCompanyEstimate() throws Exception {
        LineItemCreateRequest request = LineItemCreateRequest.builder()
                .productCode("P001").productDescription("Labour")
                .unitPrice(new BigDecimal("50")).coreOrSub(CoreOrSub.CORE)
                .quantity(new BigDecimal("1")).vatRate(new BigDecimal("0.19")).build();

        mockMvc.perform(post("/api/v1/estimates/" + estimate.getId() + "/line-items")
                        .header("Authorization", "Bearer " + anotherCompanyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= PUT /{estimateId}/line-items/{lineItemId} — link existing =============

    @Test
    void shouldLinkExistingLineItemToEstimate() throws Exception {
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(put("/api/v1/estimates/" + estimate.getId() + "/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems", hasSize(1)))
                .andExpect(jsonPath("$.lineItems[0].productCode").value("P-EXISTING"))
                .andExpect(jsonPath("$.lineItems[0].totalAmount").value(119.00))
                .andExpect(jsonPath("$.grandTotal").value(119.00));
    }

    @Test
    void shouldBeLinkIdempotent_WhenLineItemAlreadyLinked() throws Exception {
        // Link it first
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(put("/api/v1/estimates/" + estimate.getId() + "/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems", hasSize(1)));

        entityManager.flush();
        entityManager.clear();

        // Link it again — should still be 1 item, not 2
        mockMvc.perform(put("/api/v1/estimates/" + estimate.getId() + "/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems", hasSize(1)));
    }

    @Test
    void shouldReturn404WhenLinkingLineItemFromAnotherCompany() throws Exception {
        // existingLineItem belongs to `company`, but we're authenticating as `anotherCompany`
        mockMvc.perform(put("/api/v1/estimates/" + estimate.getId() + "/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenWorkerLinksLineItem() throws Exception {
        mockMvc.perform(put("/api/v1/estimates/" + estimate.getId() + "/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    // ============= DELETE /{estimateId}/line-items/{lineItemId} — unlink only =============

    @Test
    void shouldUnlinkLineItemWithoutDeletingIt() throws Exception {
        // Link the item first so we can unlink it
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(put("/api/v1/estimates/" + estimate.getId() + "/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        // Now unlink
        mockMvc.perform(delete("/api/v1/estimates/" + estimate.getId() + "/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems", hasSize(0)))
                .andExpect(jsonPath("$.grandTotal").value(0));

        // Line item still exists in the DB
        mockMvc.perform(get("/api/v1/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingLineItem.getId()));
    }

    @Test
    void shouldReturn404WhenUnlinkingLineItemNotLinkedToEstimate() throws Exception {
        // existingLineItem has never been linked
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/v1/estimates/" + estimate.getId() + "/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteEstimateAndLeaveLineItemIntact() throws Exception {
        // Link item, delete estimate, verify line item survives
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(put("/api/v1/estimates/" + estimate.getId() + "/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/v1/estimates/" + estimate.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());

        // Line item must still exist
        mockMvc.perform(get("/api/v1/line-items/" + existingLineItem.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("P-EXISTING"));
    }

    // ============= Authorization =============

    @Test
    void shouldReturn403ForWorker() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/" + estimate.getId())
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401ForInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/" + estimate.getId())
                        .header("Authorization", "Bearer bad.token.here"))
                .andExpect(status().isUnauthorized());
    }
}
