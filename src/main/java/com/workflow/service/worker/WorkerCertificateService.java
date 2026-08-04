package com.workflow.service.worker;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.worker.CertificateType;
import com.workflow.common.exception.business.EmptyFileException;
import com.workflow.common.exception.business.FileSizeLimitExceededException;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.WorkerCertificateNotFoundException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.dto.worker.ExpiringCertificateResponse;
import com.workflow.dto.worker.WorkerCertificateResponse;
import com.workflow.dto.worker.WorkerCertificateUpdateRequest;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.Worker;
import com.workflow.entity.worker.WorkerCertificate;
import com.workflow.repository.worker.WorkerCertificateRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.storage.IStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkerCertificateService implements IWorkerCertificateService {

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15 MB
    private static final int DEFAULT_EXPIRING_SOON_DAYS = 30;

    private final WorkerRepository workerRepository;
    private final WorkerCertificateRepository certificateRepository;
    private final Tika tika;
    private final IStorageService s3Service;

    @Value("${workflow.security.file.blocked-types}")
    private List<String> blockedTypes;

    private Worker getWorker(Long userId) {
        return workerRepository.findByUserId(userId)
                .orElseThrow(() -> new WorkerNotFoundException("Current user is not a registered worker"));
    }

    private Worker getWorkerInCompany(Long workerId, Long companyId) {
        return workerRepository.findByIdAndCompanyIdAndNotArchived(workerId, companyId)
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with ID: " + workerId));
    }

    private WorkerCertificate uploadCertificate(
            Worker worker,
            User uploadedBy,
            MultipartFile file,
            CertificateType type,
            String name,
            String issuingAuthority,
            LocalDate issueDate,
            LocalDate expiryDate) throws IOException {

        if (file.isEmpty()) {
            throw new EmptyFileException("Uploaded file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeLimitExceededException("Certificate file must not exceed 15 MB");
        }

        String detectedType = tika.detect(file.getInputStream());

        if (blockedTypes.contains(detectedType)) {
            throw new ForbiddenActionException("Upload is blocked for security reasons.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String safeUniqueFilename = UUID.randomUUID() + extension;

        String key = String.format(
                "companies/%d/workers/%d/certificates/%s",
                worker.getCompany().getId(),
                worker.getId(),
                safeUniqueFilename);

        s3Service.upload(key, file.getInputStream(), file.getSize(), detectedType);

        return certificateRepository.save(
                WorkerCertificate.builder()
                        .worker(worker)
                        .uploadedBy(uploadedBy)
                        .type(type)
                        .name(name)
                        .issuingAuthority(issuingAuthority)
                        .issueDate(issueDate)
                        .expiryDate(expiryDate)
                        .fileName(originalFilename)
                        .fileType(detectedType)
                        .fileUrl(key)
                        .fileSizeBytes(file.getSize())
                        .build());
    }

    @Override
    public WorkerCertificateResponse uploadOwnCertificate(
            Long workerUserId,
            MultipartFile file,
            CertificateType type,
            String name,
            String issuingAuthority,
            LocalDate issueDate,
            LocalDate expiryDate) throws IOException {

        Worker worker = getWorker(workerUserId);
        WorkerCertificate certificate = uploadCertificate(
                worker, worker.getUser(), file, type, name, issuingAuthority, issueDate, expiryDate);
        return map(certificate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerCertificateResponse> getOwnCertificates(Long workerUserId) {
        Worker worker = getWorker(workerUserId);
        return certificateRepository.findByWorkerId(worker.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public WorkerCertificateResponse updateOwnCertificate(Long workerUserId, Long certificateId, WorkerCertificateUpdateRequest request) {
        Worker worker = getWorker(workerUserId);

        WorkerCertificate certificate = certificateRepository.findByIdAndWorkerId(certificateId, worker.getId())
                .orElseThrow(() -> new WorkerCertificateNotFoundException("Certificate not found with ID: " + certificateId));

        applyUpdate(certificate, request);

        return map(certificateRepository.save(certificate));
    }

    @Override
    public void deleteOwnCertificate(Long workerUserId, Long certificateId) {
        Worker worker = getWorker(workerUserId);

        WorkerCertificate certificate = certificateRepository.findByIdAndWorkerId(certificateId, worker.getId())
                .orElseThrow(() -> new WorkerCertificateNotFoundException("Certificate not found with ID: " + certificateId));

        s3Service.delete(certificate.getFileUrl());
        certificateRepository.delete(certificate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerCertificateResponse> getCertificatesForWorker(Long workerId, Long companyId) {
        getWorkerInCompany(workerId, companyId);
        return certificateRepository.findByWorkerIdAndCompanyId(workerId, companyId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkerCertificateResponse> listCompanyCertificates(Long companyId, Pageable pageable) {
        return certificateRepository.findByCompanyId(companyId, pageable).map(this::map);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpiringCertificateResponse> getExpiringCertificates(Long companyId, int withinDays) {
        int days = withinDays > 0 ? withinDays : DEFAULT_EXPIRING_SOON_DAYS;
        LocalDate threshold = LocalDate.now().plusDays(days);

        return certificateRepository.findExpiringByCompanyId(companyId, threshold)
                .stream()
                .map(c -> new ExpiringCertificateResponse(
                        c.getId(),
                        c.getWorker().getId(),
                        c.getWorker().getName(),
                        c.getType(),
                        c.getName(),
                        c.getExpiryDate(),
                        ChronoUnit.DAYS.between(LocalDate.now(), c.getExpiryDate())))
                .toList();
    }

    @Override
    public WorkerCertificateResponse uploadCertificateForWorker(
            Long workerId,
            Long companyId,
            User admin,
            MultipartFile file,
            CertificateType type,
            String name,
            String issuingAuthority,
            LocalDate issueDate,
            LocalDate expiryDate) throws IOException {

        Worker worker = getWorkerInCompany(workerId, companyId);
        WorkerCertificate certificate = uploadCertificate(
                worker, admin, file, type, name, issuingAuthority, issueDate, expiryDate);
        return map(certificate);
    }

    @Override
    public void deleteCertificate(Long certificateId, Long workerId, Long companyId) {
        WorkerCertificate certificate = certificateRepository.findByIdAndWorkerIdAndCompanyId(certificateId, workerId, companyId)
                .orElseThrow(() -> new WorkerCertificateNotFoundException("Certificate not found with ID: " + certificateId));

        s3Service.delete(certificate.getFileUrl());
        certificateRepository.delete(certificate);
    }

    private void applyUpdate(WorkerCertificate certificate, WorkerCertificateUpdateRequest request) {
        if (request.name() != null) {
            certificate.setName(request.name());
        }
        if (request.issuingAuthority() != null) {
            certificate.setIssuingAuthority(request.issuingAuthority());
        }
        if (request.issueDate() != null) {
            certificate.setIssueDate(request.issueDate());
        }
        if (request.expiryDate() != null) {
            certificate.setExpiryDate(request.expiryDate());
        }
    }

    private WorkerCertificateResponse map(WorkerCertificate c) {
        Long daysUntilExpiry = c.getExpiryDate() == null
                ? null
                : ChronoUnit.DAYS.between(LocalDate.now(), c.getExpiryDate());
        boolean expiringSoon = daysUntilExpiry != null && daysUntilExpiry >= 0 && daysUntilExpiry <= DEFAULT_EXPIRING_SOON_DAYS;

        return new WorkerCertificateResponse(
                c.getId(),
                c.getWorker().getId(),
                c.getWorker().getName(),
                c.getType(),
                c.getName(),
                c.getIssuingAuthority(),
                c.getIssueDate(),
                c.getExpiryDate(),
                daysUntilExpiry,
                expiringSoon,
                c.getFileName(),
                s3Service.resolveFileUrl(c.getFileUrl()),
                c.getUploadedBy().getUsername(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
