package com.workflow.dto.worker;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.workflow.common.constant.worker.CertificateType;

public record WorkerCertificateResponse(
        Long id,
        Long workerId,
        String workerName,
        CertificateType type,
        String name,
        String issuingAuthority,
        LocalDate issueDate,
        LocalDate expiryDate,
        Long daysUntilExpiry,
        boolean expiringSoon,
        String fileName,
        String fileUrl,
        String uploadedByUsername,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime updatedAt
) {}
