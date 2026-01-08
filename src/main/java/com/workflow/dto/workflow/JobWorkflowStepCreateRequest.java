package com.workflow.dto.workflow;

import com.workflow.common.constant.workflow.WorkflowStepStatus;
import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowStepCreateRequest {

    private String name;
    private String description;

    /**
     * Position of the step in the job workflow
     */
    private Integer orderIndex;

    /**
     * Optional initial status.
     * Defaults to NOT_STARTED if null.
     */
    private WorkflowStepStatus status;

    /**
     * Workers assigned to this step
     */
    private Set<Long> assignedWorkerIds;
}
