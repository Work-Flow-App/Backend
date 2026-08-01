package com.workflow.controller.worker;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.worker.CertificateType;
import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.worker.ExpiringCertificateResponse;
import com.workflow.dto.worker.WorkerCertificateResponse;
import com.workflow.entity.auth.User;
import com.workflow.service.worker.IWorkerCertificateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Worker Certificates", description = "Company-facing endpoints to view and manage worker certificates")
@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
public class WorkerCertificateController {

    private final IWorkerCertificateService certificateService;

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }

    @Operation(summary = "List certificates for a specific worker")
    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{workerId}/certificates")
    public ResponseEntity<List<WorkerCertificateResponse>> getCertificatesForWorker(
            @PathVariable Long workerId, Authentication auth) {
        return ResponseEntity.ok(certificateService.getCertificatesForWorker(workerId, getCompanyId()));
    }

    @Operation(summary = "Upload a certificate on behalf of a worker")
    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping(value = "/{workerId}/certificates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkerCertificateResponse> uploadCertificateForWorker(
            @PathVariable Long workerId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") CertificateType type,
            @RequestParam("name") String name,
            @RequestParam(value = "issuingAuthority", required = false) String issuingAuthority,
            @RequestParam(value = "issueDate", required = false) LocalDate issueDate,
            @RequestParam(value = "expiryDate", required = false) LocalDate expiryDate,
            Authentication auth) throws IOException {
        User admin = (User) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificateService.uploadCertificateForWorker(
                        workerId, getCompanyId(), admin, file, type, name, issuingAuthority, issueDate, expiryDate));
    }

    @Operation(summary = "List all certificates company-wide, paginated")
    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/certificates")
    public ResponseEntity<Page<WorkerCertificateResponse>> listCompanyCertificates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(certificateService.listCompanyCertificates(getCompanyId(), pageable));
    }

    @Operation(summary = "List certificates expiring within N days, company-wide")
    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/certificates/expiring")
    public ResponseEntity<List<ExpiringCertificateResponse>> getExpiringCertificates(
            @RequestParam(defaultValue = "30") int days,
            Authentication auth) {
        return ResponseEntity.ok(certificateService.getExpiringCertificates(getCompanyId(), days));
    }

    @Operation(summary = "Delete a worker's certificate")
    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/{workerId}/certificates/{id}")
    public ResponseEntity<Void> deleteCertificate(
            @PathVariable Long workerId, @PathVariable Long id, Authentication auth) {
        certificateService.deleteCertificate(id, workerId, getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
