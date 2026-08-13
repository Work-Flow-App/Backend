package com.workflow.dto.form;

import lombok.*;

@Data
@Builder
public class FormFieldValueResponse {
    private Long id;
    private Long fieldId;
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private String roleTarget;
    private boolean required;
    private Object value; // Typed value
    private String fileUrl;
    private String fileName;
    private String options;
}