package com.workflow.dto.form;

import lombok.*;
import java.util.List;

@Data
@Builder
public class FormSubmissionResponse {
    private Long id;
    private String title;
    private String status;
    private Long templateId;
    private String templateName;
    private Long workerId;
    private String workerName;
    private List<FormFieldValueResponse> values;
}
