package com.workflow.dto.job;

import com.workflow.common.constant.job.JobStatus;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobUpdateRequest {
    private Long clientId;
    private Long assignedWorkerId;
    private Long workflowId;
    private JobStatus status;
    private boolean archived;
    private Map<Long, Object> fieldValues;
    private List<Long> assetIds;
}
