package com.workflow.dto.workflow;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowResponse {
    private Long jobId;
    private Long workflowId;
    private List<JobWorkflowStepResponse> steps;
}
