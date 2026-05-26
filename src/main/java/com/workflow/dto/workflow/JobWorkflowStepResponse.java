package com.workflow.dto.workflow;

import java.time.LocalDateTime;
import java.util.Set;

import com.workflow.common.constant.workflow.SlaStatus;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowStepResponse {

    private Long id;
    private String name;
    private String description;
    private Integer orderIndex;
    private WorkflowStepStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private Integer expectedDurationMinutes;
    private Integer maximumDurationMinutes;
    private SlaStatus slaStatus; // Dynamically calculated

    private Set<Long> assignedWorkerIds;
}