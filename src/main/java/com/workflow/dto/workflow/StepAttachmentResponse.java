package com.workflow.dto.workflow;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.workflow.common.constant.workflow.StepDiscussionType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepAttachmentResponse {
    private Long id;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private String description;
    private StepDiscussionType type;
    private Long uploadedBy;
    private String uploadedByUsername;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
}
