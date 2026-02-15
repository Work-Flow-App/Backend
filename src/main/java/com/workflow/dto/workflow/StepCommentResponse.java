package com.workflow.dto.workflow;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
