package com.workflow.dto.workflow;

import com.workflow.common.constant.workflow.StepDiscussionType;

import lombok.Data;

@Data
public class StepAttachmentUpdateRequest {

    private String fileName;
    private String description;
    private StepDiscussionType type;
}