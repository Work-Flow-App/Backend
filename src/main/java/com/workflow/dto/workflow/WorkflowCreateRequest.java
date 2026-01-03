package com.workflow.dto.workflow;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowCreateRequest {
    private String name;
    private String description;
}
