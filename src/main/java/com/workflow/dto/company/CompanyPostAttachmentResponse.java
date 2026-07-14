package com.workflow.dto.company;

public record CompanyPostAttachmentResponse(
        Long id,
        String fileUrl,
        String fileName,
        String fileType
) {}