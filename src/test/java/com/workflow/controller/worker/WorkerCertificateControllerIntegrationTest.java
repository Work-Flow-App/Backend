package com.workflow.controller.worker;

import com.workflow.AbstractControllerIntegrationTest;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.worker.CertificateType;
import com.workflow.entity.company.Company;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.Worker;
import com.workflow.entity.worker.WorkerCertificate;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.worker.WorkerCertificateRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkerCertificateControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private WorkerCertificateRepository certificateRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Worker worker1;
    private Worker worker2;
    private WorkerCertificate existingCertificate;
    private String companyAToken;
    private String companyBToken;
    private String worker1Token;
    private String worker2Token;

    @BeforeEach
    void setUp() {
        certificateRepository.deleteAll();
        workerRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        when(storageService.resolveFileUrl(anyString())).thenReturn("https://fake-s3.test/cert.pdf");
        doNothing().when(storageService).upload(anyString(), any(), anyLong(), anyString());
        doNothing().when(storageService).delete(anyString());

        User companyAUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("certcompanyA")
                .password(passwordEncoder.encode("password")).email("certcompanyA@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User companyBUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("certcompanyB")
                .password(passwordEncoder.encode("password")).email("certcompanyB@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User worker1User = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("certworker1")
                .password(passwordEncoder.encode("password")).email("certworker1@test.com")
                .role(Role.WORKER).enabled(true).build());

        User worker2User = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("certworker2")
                .password(passwordEncoder.encode("password")).email("certworker2@test.com")
                .role(Role.WORKER).enabled(true).build());

        Company companyA = companyRepository.save(Company.builder()
                .name("Company A").user(companyAUser).email("certcompanyA@test.com").archived(false).build());

        Company companyB = companyRepository.save(Company.builder()
                .name("Company B").user(companyBUser).email("certcompanyB@test.com").archived(false).build());

        createCompanyMember(companyA, companyAUser, CompanyRole.COMPANY_ADMIN);
        createCompanyMember(companyB, companyBUser, CompanyRole.COMPANY_ADMIN);

        worker1 = workerRepository.save(Worker.builder()
                .name("Worker One").company(companyA).user(worker1User).archived(false).build());

        worker2 = workerRepository.save(Worker.builder()
                .name("Worker Two").company(companyA).user(worker2User).archived(false).build());

        existingCertificate = certificateRepository.save(WorkerCertificate.builder()
                .worker(worker1)
                .uploadedBy(worker1User)
                .type(CertificateType.SAFETY)
                .name("First Aid")
                .expiryDate(LocalDate.now().plusDays(10))
                .fileName("first-aid.pdf")
                .fileType("application/pdf")
                .fileUrl("companies/1/workers/1/certificates/existing.pdf")
                .build());

        companyAToken = jwtService.generateToken(companyAUser);
        companyBToken = jwtService.generateToken(companyBUser);
        worker1Token = jwtService.generateToken(worker1User);
        worker2Token = jwtService.generateToken(worker2User);
    }

    // ============= POST /api/v1/worker/certificates (self) =============

    @Test
    void shouldUploadOwnCertificateSuccessfully() throws Exception {
        byte[] fileContent = "content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", fileContent);

        Long companyId = worker1.getCompany().getId();
        long storageUsedBeforeUpload = companyRepository.findById(companyId).orElseThrow().getStorageUsedBytes();

        mockMvc.perform(multipart("/api/v1/worker/certificates")
                        .file(file)
                        .param("type", "LICENSE")
                        .param("name", "Driving License")
                        .param("issuingAuthority", "DVLA")
                        .param("issueDate", "2026-01-01")
                        .param("expiryDate", "2026-12-31")
                        .header("Authorization", "Bearer " + worker1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Driving License"))
                .andExpect(jsonPath("$.workerId").value(worker1.getId()))
                .andExpect(jsonPath("$.uploadedByUsername").value("certworker1"));

        long storageUsedAfterUpload = companyRepository.findById(companyId).orElseThrow().getStorageUsedBytes();
        assertThat(storageUsedAfterUpload - storageUsedBeforeUpload).isEqualTo(fileContent.length);
    }

    // ============= GET /api/v1/worker/certificates (self) - JOIN FETCH / OSIV regression =============

    @Test
    void shouldListOwnCertificatesWithoutLazyInitializationException() throws Exception {
        mockMvc.perform(get("/api/v1/worker/certificates")
                        .header("Authorization", "Bearer " + worker1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].workerId").value(worker1.getId()))
                .andExpect(jsonPath("$[0].uploadedByUsername").value("certworker1"));
    }

    // ============= IDOR: self-service cannot touch another worker's certificate =============

    @Test
    void shouldReturn404WhenAnotherWorkerUpdatesCertificate() throws Exception {
        mockMvc.perform(patch("/api/v1/worker/certificates/" + existingCertificate.getId())
                        .header("Authorization", "Bearer " + worker2Token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isNotFound());
    }

    // ============= Admin endpoints =============

    @Test
    void shouldUploadCertificateForWorkerAsAdmin() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "onboarding.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/workers/" + worker1.getId() + "/certificates")
                        .file(file)
                        .param("type", "TRADE_QUALIFICATION")
                        .param("name", "Electrician Certificate")
                        .header("Authorization", "Bearer " + companyAToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workerId").value(worker1.getId()))
                .andExpect(jsonPath("$.uploadedByUsername").value("certcompanyA"));
    }

    @Test
    void shouldListCompanyCertificatesPaginated() throws Exception {
        mockMvc.perform(get("/api/v1/workers/certificates")
                        .header("Authorization", "Bearer " + companyAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].workerName").value("Worker One"));
    }

    @Test
    void shouldReturn404WhenViewingWorkerFromAnotherCompany() throws Exception {
        mockMvc.perform(get("/api/v1/workers/" + worker1.getId() + "/certificates")
                        .header("Authorization", "Bearer " + companyBToken))
                .andExpect(status().isNotFound());
    }

    // ============= DELETE regression: must be scoped by workerId, not just certificate id =============

    @Test
    void shouldReturn404WhenDeletingCertificateWithMismatchedWorkerId() throws Exception {
        // existingCertificate belongs to worker1, not worker2 - the URL claims worker2.
        mockMvc.perform(delete("/api/v1/workers/" + worker2.getId() + "/certificates/" + existingCertificate.getId())
                        .header("Authorization", "Bearer " + companyAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteCertificateSuccessfullyWhenWorkerIdMatches() throws Exception {
        mockMvc.perform(delete("/api/v1/workers/" + worker1.getId() + "/certificates/" + existingCertificate.getId())
                        .header("Authorization", "Bearer " + companyAToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldDecrementStorageUsedBytesWhenDeletingCertificateWithFileSize() throws Exception {
        WorkerCertificate sizedCertificate = certificateRepository.save(WorkerCertificate.builder()
                .worker(worker1)
                .uploadedBy(worker1.getUser())
                .type(CertificateType.SAFETY)
                .name("Sized Certificate")
                .fileName("sized.pdf")
                .fileType("application/pdf")
                .fileUrl("companies/1/workers/1/certificates/sized.pdf")
                .fileSizeBytes(750L)
                .build());

        Long companyId = worker1.getCompany().getId();
        companyRepository.findById(companyId).ifPresent(c -> {
            c.setStorageUsedBytes(2000L);
            companyRepository.save(c);
        });

        mockMvc.perform(delete("/api/v1/workers/" + worker1.getId() + "/certificates/" + sizedCertificate.getId())
                        .header("Authorization", "Bearer " + companyAToken))
                .andExpect(status().isNoContent());

        long storageUsedAfterDelete = companyRepository.findById(companyId).orElseThrow().getStorageUsedBytes();
        assertThat(storageUsedAfterDelete).isEqualTo(1250L);
    }
}
