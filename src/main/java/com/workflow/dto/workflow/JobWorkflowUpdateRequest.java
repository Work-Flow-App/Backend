package com.workflow.dto.workflow;

import lombok.*;
import java.util.List;

import com.workflow.common.constant.workflow.WorkflowStepStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowUpdateRequest {
    private WorkflowStepStatus status; // Optional: directly set workflow status
    private List<JobWorkflowStepUpdateRequest> steps; // Optional: update multiple steps
}
