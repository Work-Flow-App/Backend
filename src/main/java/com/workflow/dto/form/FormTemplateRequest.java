package com.workflow.dto.form;

import lombok.*;
import java.util.List;

@Data
@Builder
public class FormTemplateRequest {
    private Long id;
    private String name;
    private String description;
    private Integer version;
    private Long parentTemplateId;
    private boolean archived;
    private List<FormFieldDto> fields;
}