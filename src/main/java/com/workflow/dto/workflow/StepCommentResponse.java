package com.workflow.dto.workflow;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.workflow.common.constant.workflow.StepDiscussionType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepCommentResponse {
    private Long id;
    private String content;
    private StepDiscussionType type;
    private Long authorId;
    private String authorUsername;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime updatedAt;
}
