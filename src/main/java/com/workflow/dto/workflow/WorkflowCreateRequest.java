package com.workflow.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowCreateRequest {
    @NotBlank(message = "Workflow name is required")
    private String name;
    private String description;
}
