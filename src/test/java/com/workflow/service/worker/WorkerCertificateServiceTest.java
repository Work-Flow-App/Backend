package com.workflow.service.worker;

import com.workflow.common.constant.Role;
import com.workflow.common.constant.worker.CertificateType;
import com.workflow.common.exception.business.EmptyFileException;
import com.workflow.common.exception.business.FileSizeLimitExceededException;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.WorkerCertificateNotFoundException;
import com.workflow.dto.worker.WorkerCertificateResponse;
import com.workflow.dto.worker.WorkerCertificateUpdateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.Worker;
import com.workflow.entity.worker.WorkerCertificate;
import com.workflow.repository.worker.WorkerCertificateRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.storage.IStorageService;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerCertificateServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private WorkerCertificateRepository certificateRepository;

    @Mock
    private Tika tika;

    @Mock
    private IStorageService s3Service;

    @InjectMocks
    private WorkerCertificateService certificateService;

    private User workerUser;
    private Worker worker;
    private WorkerCertificate certificate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(certificateService, "blockedTypes",
                List.of("application/x-msdownload", "text/html"));

        User companyUser = User.builder().id(1L).uuid("company-uuid").username("companyowner")
                .role(Role.COMPANY).enabled(true).build();

        Company company = Company.builder().id(1L).name("Test Company").user(companyUser).build();

        workerUser = User.builder().id(2L).uuid("worker-uuid").username("worker1")
                .role(Role.WORKER).enabled(true).build();

        worker = Worker.builder().id(10L).name("John Worker").company(company).user(workerUser)
                .archived(false).build();

        certificate = WorkerCertificate.builder()
                .id(100L)
                .worker(worker)
                .uploadedBy(workerUser)
                .type(CertificateType.SAFETY)
                .name("First Aid")
                .expiryDate(LocalDate.now().plusDays(10))
                .fileName("cert.pdf")
                .fileType("application/pdf")
                .fileUrl("companies/1/workers/10/certificates/uuid.pdf")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ============= uploadOwnCertificate Tests =============

    @Test
    void uploadOwnCertificate_ShouldUploadSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cert.pdf", "application/pdf", "content".getBytes());
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));
        when(tika.detect(any(java.io.InputStream.class))).thenReturn("application/pdf");
        when(certificateRepository.save(any(WorkerCertificate.class))).thenReturn(certificate);

        WorkerCertificateResponse response = certificateService.uploadOwnCertificate(
                2L, file, CertificateType.SAFETY, "First Aid", "Red Cross", LocalDate.now(), LocalDate.now().plusDays(10));

        assertThat(response).isNotNull();
        assertThat(response.expiringSoon()).isTrue();
        verify(s3Service).upload(anyString(), any(), anyLong(), eq("application/pdf"));
        verify(certificateRepository).save(any(WorkerCertificate.class));
    }

    @Test
    void uploadOwnCertificate_ShouldThrow_WhenFileEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "cert.pdf", "application/pdf", new byte[0]);
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));

        assertThatThrownBy(() -> certificateService.uploadOwnCertificate(
                2L, emptyFile, CertificateType.SAFETY, "First Aid", null, null, null))
                .isInstanceOf(EmptyFileException.class);

        verify(certificateRepository, never()).save(any());
        verify(s3Service, never()).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void uploadOwnCertificate_ShouldThrow_WhenFileTooLarge() {
        byte[] oversized = new byte[16 * 1024 * 1024]; // 16MB > 15MB limit
        MockMultipartFile bigFile = new MockMultipartFile("file", "cert.pdf", "application/pdf", oversized);
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));

        assertThatThrownBy(() -> certificateService.uploadOwnCertificate(
                2L, bigFile, CertificateType.SAFETY, "First Aid", null, null, null))
                .isInstanceOf(FileSizeLimitExceededException.class);

        verify(certificateRepository, never()).save(any());
    }

    @Test
    void uploadOwnCertificate_ShouldThrow_WhenTypeBlocked() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/x-msdownload", "bad".getBytes());
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));
        when(tika.detect(any(java.io.InputStream.class))).thenReturn("application/x-msdownload");

        assertThatThrownBy(() -> certificateService.uploadOwnCertificate(
                2L, file, CertificateType.SAFETY, "First Aid", null, null, null))
                .isInstanceOf(ForbiddenActionException.class);

        verify(certificateRepository, never()).save(any());
        verify(s3Service, never()).upload(anyString(), any(), anyLong(), anyString());
    }

    // ============= updateOwnCertificate Tests =============

    @Test
    void updateOwnCertificate_ShouldThrow_WhenCertificateNotOwnedByWorker() {
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));
        when(certificateRepository.findByIdAndWorkerId(100L, 10L)).thenReturn(Optional.empty());

        WorkerCertificateUpdateRequest request = new WorkerCertificateUpdateRequest("New Name", null, null, null);

        assertThatThrownBy(() -> certificateService.updateOwnCertificate(2L, 100L, request))
                .isInstanceOf(WorkerCertificateNotFoundException.class);

        verify(certificateRepository, never()).save(any());
    }

    // ============= deleteCertificate (admin) Tests =============

    @Test
    void deleteCertificate_ShouldScopeByWorkerIdAndCompanyId() {
        when(certificateRepository.findByIdAndWorkerIdAndCompanyId(100L, 10L, 1L)).thenReturn(Optional.of(certificate));

        certificateService.deleteCertificate(100L, 10L, 1L);

        verify(s3Service).delete(certificate.getFileUrl());
        verify(certificateRepository).delete(certificate);
    }

    @Test
    void deleteCertificate_ShouldThrow_WhenCertificateBelongsToDifferentWorker() {
        // Regression test: certificate 100 belongs to worker 10, not worker 99 -
        // the lookup must be scoped by workerId, not just companyId.
        when(certificateRepository.findByIdAndWorkerIdAndCompanyId(100L, 99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.deleteCertificate(100L, 99L, 1L))
                .isInstanceOf(WorkerCertificateNotFoundException.class);

        verify(s3Service, never()).delete(anyString());
        verify(certificateRepository, never()).delete(any());
    }

    // ============= getExpiringCertificates Tests =============

    @Test
    void getExpiringCertificates_ShouldUseDefaultDays_WhenInvalidInput() {
        when(certificateRepository.findExpiringByCompanyId(eq(1L), any(LocalDate.class))).thenReturn(List.of(certificate));

        certificateService.getExpiringCertificates(1L, 0);

        verify(certificateRepository).findExpiringByCompanyId(eq(1L), eq(LocalDate.now().plusDays(30)));
    }
}
