package com.workflow.dto.workflow;

import lombok.*;
import java.util.List;
import java.util.Set;

import com.workflow.common.constant.workflow.WorkflowStepStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowResponse {
    private Long id;
    private Long jobId;
    private Long workflowId;
    private List<JobWorkflowStepResponse> steps;
    private WorkflowStepStatus status;
    private Set<Long> workerIds;
}
