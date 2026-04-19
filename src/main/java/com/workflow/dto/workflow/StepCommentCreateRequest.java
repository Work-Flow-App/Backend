package com.workflow.dto.workflow;

import com.workflow.common.constant.workflow.StepDiscussionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
public class StepCommentCreateRequest {
    @NotBlank(message = "Comment content is required")
    private String content;
    @NotNull(message = "Comment type is required")
    private StepDiscussionType type;
}