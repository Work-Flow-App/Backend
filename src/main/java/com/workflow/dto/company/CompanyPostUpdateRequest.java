package com.workflow.dto.company;

import java.util.List;

public record CompanyPostUpdateRequest(
        String content,
        Boolean isPublic,
        List<Long> attachmentIdsToDelete // IDs of existing files the user wants to remove
) {
}