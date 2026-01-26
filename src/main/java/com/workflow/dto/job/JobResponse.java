package com.workflow.dto.job;

import com.workflow.common.constant.job.JobStatus;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {
    private Long id;
    private Long companyId;
    private Long templateId;
    private Long clientId;
    private Long assignedWorkerId;
    private Long workflowId;
    private JobStatus status;
    private boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<Long, FieldValueResponse> fieldValues;
    private List<Long> assetIds;
}
