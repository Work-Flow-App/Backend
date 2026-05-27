package com.workflow.dto.workflow;

import com.workflow.common.constant.workflow.WorkflowStepStatus;
import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowStepUpdateRequest {

    /**
     * Existing JobWorkflowStep ID (required)
     */
    private Long id;

    private String name;
    private String description;

    /**
     * Change order to reorder steps
     */
    private Integer orderIndex;

    private WorkflowStepStatus status;

    /**
     * Replace assigned workers.
     * null → do not change
     * empty set → remove all workers
     */
    private Set<Long> assignedWorkerIds;
    private Integer expectedDurationMinutes;
    private Integer maximumDurationMinutes;
}
