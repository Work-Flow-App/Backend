package com.workflow.dto.worker;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record WorkerCertificateUpdateRequest(
        @Size(max = 150, message = "Name cannot exceed 150 characters")
        String name,

        @Size(max = 150, message = "Issuing authority cannot exceed 150 characters")
        String issuingAuthority,

        LocalDate issueDate,

        LocalDate expiryDate
) {}
