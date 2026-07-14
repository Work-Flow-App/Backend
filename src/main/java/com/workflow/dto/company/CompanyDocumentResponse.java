package com.workflow.dto.company;

import com.workflow.common.constant.company.CompanyDocumentType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CompanyDocumentResponse(
        Long id,
        String title,
        String description,
        CompanyDocumentType type,
        String fileUrl,
        String fileName,
        LocalDate validityStartDate,
        LocalDate validityEndDate,
        boolean isPublic,
        LocalDateTime createdAt
) {}