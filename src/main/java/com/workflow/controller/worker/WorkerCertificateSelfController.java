package com.workflow.controller.worker;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.worker.CertificateType;
import com.workflow.dto.worker.WorkerCertificateResponse;
import com.workflow.dto.worker.WorkerCertificateUpdateRequest;
import com.workflow.entity.auth.User;
import com.workflow.service.worker.IWorkerCertificateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Worker Certificates (Self-Service)", description = "Endpoints for workers to manage their own certificates")
@RestController
@RequestMapping("/api/v1/worker/certificates")
@RequiredArgsConstructor
public class WorkerCertificateSelfController {

    private final IWorkerCertificateService certificateService;

    private Long getUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    @Operation(summary = "Upload a certificate for the current worker")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkerCertificateResponse> uploadOwnCertificate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") CertificateType type,
            @RequestParam(value = "customTypeLabel", required = false) String customTypeLabel,
            @RequestParam("name") String name,
            @RequestParam(value = "issuingAuthority", required = false) String issuingAuthority,
            @RequestParam(value = "issueDate", required = false) LocalDate issueDate,
            @RequestParam(value = "expiryDate", required = false) LocalDate expiryDate,
            Authentication auth) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificateService.uploadOwnCertificate(
                        getUserId(auth), file, type, customTypeLabel, name, issuingAuthority, issueDate, expiryDate));
    }

    @Operation(summary = "List the current worker's certificates")
    @GetMapping
    public ResponseEntity<List<WorkerCertificateResponse>> getOwnCertificates(Authentication auth) {
        return ResponseEntity.ok(certificateService.getOwnCertificates(getUserId(auth)));
    }

    @Operation(summary = "Get a single own certificate")
    @GetMapping("/{id}")
    public ResponseEntity<WorkerCertificateResponse> getOwnCertificate(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(certificateService.getOwnCertificate(getUserId(auth), id));
    }

    @Operation(summary = "Update metadata of an own certificate")
    @PatchMapping("/{id}")
    public ResponseEntity<WorkerCertificateResponse> updateOwnCertificate(
            @PathVariable Long id,
            @Valid @RequestBody WorkerCertificateUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(certificateService.updateOwnCertificate(getUserId(auth), id, request));
    }

    @Operation(summary = "Delete an own certificate")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOwnCertificate(@PathVariable Long id, Authentication auth) {
        certificateService.deleteOwnCertificate(getUserId(auth), id);
        return ResponseEntity.noContent().build();
    }
}
