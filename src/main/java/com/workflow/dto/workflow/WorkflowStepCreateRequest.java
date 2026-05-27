package com.workflow.dto.workflow;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepCreateRequest {
    private Long workflowId;
    private String name;
    private String description;
    private Integer orderIndex;
    private boolean optional;
    private Integer expectedDurationMinutes;
    private Integer maximumDurationMinutes;
}