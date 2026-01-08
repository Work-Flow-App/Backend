package com.workflow.dto.workflow;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepBulkRequest {

    private Long id; // null = new step
    private String name;
    private String description;
    private Integer orderIndex;
    private boolean optional;
}
