package com.workflow.dto.jobtemplate;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTemplateCreateRequest {
    private String name;
    private String description;

    @Builder.Default
    private Boolean isDefault = false;
}
