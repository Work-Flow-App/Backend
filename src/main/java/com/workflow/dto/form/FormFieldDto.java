package com.workflow.dto.form;

import com.workflow.common.constant.form.FormFieldType;
import com.workflow.common.constant.form.FormRoleTarget;
import lombok.*;

@Data
@Builder
public class FormFieldDto {
    private Long id;
    private String name;
    private String label;
    private FormFieldType type;
    private FormRoleTarget roleTarget;
    private boolean required;
    private String options;
    private Integer orderIndex;
}