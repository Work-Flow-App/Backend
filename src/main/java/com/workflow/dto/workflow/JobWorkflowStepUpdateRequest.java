package com.workflow.dto.workflow;

import java.util.Set;

import com.workflow.common.constant.workflow.WorkflowStepStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowStepUpdateRequest {
    private Long id;
    private WorkflowStepStatus status;
    private Set<Long> assignedWorkerIds;
}
