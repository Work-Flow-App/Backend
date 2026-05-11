package com.workflow.controller.invoice;

import com.workflow.AbstractControllerIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CoreOrSub;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.dto.invoice.InvoiceCreateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.customer.Customer;
import com.workflow.entity.financial.Estimate;
import com.workflow.entity.financial.Invoice;
import com.workflow.entity.financial.InvoiceLineItemSnapshot;
import com.workflow.entity.financial.LineItem;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.auth.User;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.customer.CustomerRepository;
import com.workflow.repository.financial.EstimateRepository;
import com.workflow.repository.financial.InvoiceRepository;
import com.workflow.repository.financial.LineItemRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.job.JobTemplateRepository;
import com.workflow.service.auth.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InvoiceControllerIntegrationTest extends AbstractControllerIntegrationTest {

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
    @Autowired private InvoiceRepository invoiceRepository;


    private Company company;
    private Company anotherCompany;
    private Estimate estimate;
    private LineItem linkedLineItem;
    private Invoice existingInvoice;

    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        lineItemRepository.deleteAll();
        estimateRepository.deleteAll();
        jobRepository.deleteAll();
        jobTemplateRepository.deleteAll();
        customerRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        // Mock S3 and PDF renderer
        when(pdfRenderer.render(any())).thenReturn(new byte[]{1, 2, 3});
        doNothing().when(storageService).upload(anyString(), any(), anyLong(), anyString());
        when(storageService.generatePresignedUrl(anyString())).thenReturn("https://fake-s3.test/invoice.pdf");
        when(storageService.resolveFileUrl(anyString())).thenReturn("https://fake-s3.test/invoice.pdf");

        User companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("invoiceowner")
                .password(passwordEncoder.encode("password")).email("invoiceowner@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anotherinvoiceowner")
                .password(passwordEncoder.encode("password")).email("anotherinvoice@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("invoiceworker")
                .password(passwordEncoder.encode("password")).email("invoiceworker@test.com")
                .role(Role.WORKER).enabled(true).build());

        company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("invoiceowner@test.com").archived(false).build());

        anotherCompany = companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("anotherinvoice@test.com").archived(false).build());

        JobTemplate template = jobTemplateRepository.save(JobTemplate.builder()
                .name("Default Template").company(company).build());

        Customer customer = customerRepository.save(Customer.builder()
                .name("Test Customer").company(company).email("customer@test.com").build());

        Job job = jobRepository.save(Job.builder()
                .template(template).company(company).customer(customer)
                .status(JobStatus.NEW).archived(false).build());

        // Linked line item (part of the estimate)
        linkedLineItem = lineItemRepository.save(LineItem.builder()
                .company(company).productCode("INV-001").productDescription("Invoice Item")
                .unitPrice(new BigDecimal("100.00")).coreOrSub(CoreOrSub.CORE)
                .quantity(new BigDecimal("2.0000")).vatRate(new BigDecimal("20.00"))
                .netAmount(new BigDecimal("200.00")).vatAmount(new BigDecimal("40.00"))
                .totalAmount(new BigDecimal("240.00")).invoiced(false).build());

        estimate = estimateRepository.save(Estimate.builder()
                .job(job).company(company).notes("Estimate for invoice test").build());
        estimate.getLineItems().add(linkedLineItem);
        estimate = estimateRepository.save(estimate);

        // Pre-existing invoice
        InvoiceLineItemSnapshot snap = InvoiceLineItemSnapshot.builder()
                .sourceLineItemId(linkedLineItem.getId())
                .productCode(linkedLineItem.getProductCode())
                .productDescription(linkedLineItem.getProductDescription())
                .additionalDetails(linkedLineItem.getAdditionalDetails())
                .unitPrice(linkedLineItem.getUnitPrice())
                .coreOrSub(linkedLineItem.getCoreOrSub())
                .quantity(linkedLineItem.getQuantity())
                .vatRate(linkedLineItem.getVatRate())
                .netAmount(linkedLineItem.getNetAmount())
                .vatAmount(linkedLineItem.getVatAmount())
                .totalAmount(linkedLineItem.getTotalAmount())
                .build();

        Invoice invoiceToBuild = Invoice.builder()
                .estimate(estimate)
                .company(company)
                .invoiceNumber("INV-000001")
                .s3Key("invoices/INV-000001.pdf")
                .lineItemSnapshots(new ArrayList<>(List.of(snap)))
                .totalNet(new BigDecimal("200.00"))
                .totalVat(new BigDecimal("40.00"))
                .grandTotal(new BigDecimal("240.00"))
                .dueDate(LocalDate.now().plusDays(30))
                .build();
        snap.setInvoice(invoiceToBuild);
        existingInvoice = invoiceRepository.save(invoiceToBuild);

        companyToken = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken = jwtService.generateToken(workerUser);
    }

    // ============= GET /api/v1/estimates/invoices =============

    @Test
    void shouldGetAllInvoicesSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/invoices")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(existingInvoice.getId()))
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-000001"))
                .andExpect(jsonPath("$[0].grandTotal").value(240.00));
    }

    @Test
    void shouldReturnEmptyListForCompanyWithNoInvoices() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/invoices")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn403ForMissingAuthOnGetAllInvoices() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/invoices"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401ForInvalidTokenOnGetAllInvoices() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/invoices")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForWorkerRoleOnGetAllInvoices() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/invoices")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    // ============= POST /api/v1/estimates/{estimateId}/invoice =============

    @Test
    void shouldGenerateInvoiceSuccessfully() throws Exception {
        entityManager.flush();
        entityManager.clear();

        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setLineItemIds(List.of(linkedLineItem.getId()));
        request.setDueDate(LocalDate.now().plusDays(30));
        request.setReference("PO-12345");

        mockMvc.perform(post("/api/v1/estimates/" + estimate.getId() + "/invoice")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estimateId").value(estimate.getId()))
                .andExpect(jsonPath("$.invoiceNumber").exists())
                .andExpect(jsonPath("$.lineItems").isArray())
                .andExpect(jsonPath("$.grandTotal").value(greaterThan(0.0)));
    }

    @Test
    void shouldReturn400WhenNoLineItemsProvided() throws Exception {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setLineItemIds(List.of());

        mockMvc.perform(post("/api/v1/estimates/" + estimate.getId() + "/invoice")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenGeneratingInvoiceForNonExistentEstimate() throws Exception {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setLineItemIds(List.of(linkedLineItem.getId()));

        mockMvc.perform(post("/api/v1/estimates/99999/invoice")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenGeneratingInvoiceForAnotherCompanyEstimate() throws Exception {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setLineItemIds(List.of(linkedLineItem.getId()));

        mockMvc.perform(post("/api/v1/estimates/" + estimate.getId() + "/invoice")
                        .header("Authorization", "Bearer " + anotherCompanyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/estimates/{estimateId}/invoices =============

    @Test
    void shouldGetInvoicesForEstimateSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/" + estimate.getId() + "/invoices")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estimateId").value(estimate.getId()))
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-000001"));
    }

    @Test
    void shouldReturnEmptyListForEstimateWithNoInvoices() throws Exception {
        // Create a second estimate with no invoices
        Customer anotherCustomer = customerRepository.save(Customer.builder()
                .name("Another Customer").company(company).email("another@test.com").build());
        Job anotherJob = jobRepository.save(Job.builder()
                .template(jobTemplateRepository.findAll().get(0))
                .company(company).customer(anotherCustomer)
                .status(JobStatus.NEW).archived(false).build());
        Estimate emptyEstimate = estimateRepository.save(Estimate.builder()
                .job(anotherJob).company(company).notes("Empty").build());

        mockMvc.perform(get("/api/v1/estimates/" + emptyEstimate.getId() + "/invoices")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn404WhenGettingInvoicesForAnotherCompanyEstimate() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/" + estimate.getId() + "/invoices")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/estimates/invoices/{invoiceId} =============

    @Test
    void shouldGetInvoiceByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/invoices/" + existingInvoice.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingInvoice.getId()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-000001"))
                .andExpect(jsonPath("$.estimateId").value(estimate.getId()))
                .andExpect(jsonPath("$.grandTotal").value(240.00))
                .andExpect(jsonPath("$.lineItems").isArray());
    }

    @Test
    void shouldReturn404WhenInvoiceNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/invoices/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAccessingAnotherCompanyInvoice() throws Exception {
        mockMvc.perform(get("/api/v1/estimates/invoices/" + existingInvoice.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }
}
