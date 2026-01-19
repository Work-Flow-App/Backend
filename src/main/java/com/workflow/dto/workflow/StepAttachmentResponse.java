package com.workflow.dto.workflow;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepAttachmentResponse {
    private Long id;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
