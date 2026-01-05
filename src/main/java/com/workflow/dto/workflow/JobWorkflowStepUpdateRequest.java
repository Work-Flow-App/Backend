package com.workflow.dto.workflow;

import com.workflow.common.constant.workflow.WorkflowStepStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowStepUpdateRequest {
    private Long id;
    private WorkflowStepStatus status;
    private Long assignedWorkerId; // optional, can assign worker
}
