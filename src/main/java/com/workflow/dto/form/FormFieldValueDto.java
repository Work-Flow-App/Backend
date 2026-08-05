package com.workflow.dto.form;

import lombok.*;

@Data
@Builder
public class FormFieldValueDto {
    private Long fieldId;
    private String stringValue;
    private Boolean booleanValue;
    private String dateValue; // ISO string
    private String jsonValue;
}
