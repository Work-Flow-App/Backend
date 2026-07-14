package com.workflow.dto.company;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record CompanyPostResponse(
        Long id,
        String content,
        boolean isPublic,
        String authorName,
        List<CompanyPostAttachmentResponse> attachments,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime createdAt
) {}