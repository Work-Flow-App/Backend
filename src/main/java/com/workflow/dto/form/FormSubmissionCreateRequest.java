package com.workflow.dto.form;

import lombok.*;

@Data
@Builder
public class FormSubmissionCreateRequest {
    private Long templateId;
    private String title;
    private Long workerId;
    private Long jobId; 
}
