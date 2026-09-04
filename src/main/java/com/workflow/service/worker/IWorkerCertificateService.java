package com.workflow.service.worker;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.worker.CertificateType;
import com.workflow.dto.worker.ExpiringCertificateResponse;
import com.workflow.dto.worker.WorkerCertificateResponse;
import com.workflow.dto.worker.WorkerCertificateUpdateRequest;
import com.workflow.entity.auth.User;

public interface IWorkerCertificateService {

    // Self-service (worker managing their own certificates)
    WorkerCertificateResponse uploadOwnCertificate(
            Long workerUserId,
            MultipartFile file,
            CertificateType type,
            String customTypeLabel,
            String name,
            String issuingAuthority,
            LocalDate issueDate,
            LocalDate expiryDate) throws IOException;

    List<WorkerCertificateResponse> getOwnCertificates(Long workerUserId);

    WorkerCertificateResponse getOwnCertificate(Long workerUserId, Long certificateId);

    WorkerCertificateResponse updateOwnCertificate(Long workerUserId, Long certificateId, WorkerCertificateUpdateRequest request);

    void deleteOwnCertificate(Long workerUserId, Long certificateId);

    // Admin/company-facing
    List<WorkerCertificateResponse> getCertificatesForWorker(Long workerId, Long companyId);

    WorkerCertificateResponse getCertificateForWorker(Long certificateId, Long workerId, Long companyId);

    Page<WorkerCertificateResponse> listCompanyCertificates(Long companyId, Pageable pageable);

    List<ExpiringCertificateResponse> getExpiringCertificates(Long companyId, int withinDays);

    WorkerCertificateResponse uploadCertificateForWorker(
            Long workerId,
            Long companyId,
            User admin,
            MultipartFile file,
            CertificateType type,
            String customTypeLabel,
            String name,
            String issuingAuthority,
            LocalDate issueDate,
            LocalDate expiryDate) throws IOException;

    void deleteCertificate(Long certificateId, Long workerId, Long companyId);
}
