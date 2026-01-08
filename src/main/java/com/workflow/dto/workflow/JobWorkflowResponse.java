package com.workflow.dto.workflow;

import lombok.*;
import java.util.List;

import com.workflow.common.constant.workflow.WorkflowStepStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowResponse {
    private Long id;
    private Long jobId;
    private List<JobWorkflowStepResponse> steps;
    private WorkflowStepStatus status;
}
