package com.workflow.dto.worker;

import java.time.LocalDate;

import com.workflow.common.constant.worker.CertificateType;

public record ExpiringCertificateResponse(
        Long certificateId,
        Long workerId,
        String workerName,
        CertificateType type,
        String name,
        LocalDate expiryDate,
        long daysUntilExpiry
) {}
