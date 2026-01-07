package com.workflow.dto.workflow;

import java.util.List;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowBulkUpdateRequest {

    private String name;
    private String description;

    private List<WorkflowStepBulkRequest> steps;
}
