package com.workflow.dto.jobtemplate;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTemplateCreateRequest {
    @NotBlank(message = "Template name is required")
    private String name;
    private String description;

    @Builder.Default
    private Boolean isDefault = false;
}
