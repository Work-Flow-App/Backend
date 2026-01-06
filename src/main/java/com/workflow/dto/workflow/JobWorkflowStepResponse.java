package com.workflow.dto.workflow;

import java.time.LocalDateTime;
import java.util.Set;

import com.workflow.common.constant.workflow.WorkflowStepStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowStepResponse {

    private Long id;
    private String name;
    private WorkflowStepStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private Set<Long> assignedWorkerIds;
}
