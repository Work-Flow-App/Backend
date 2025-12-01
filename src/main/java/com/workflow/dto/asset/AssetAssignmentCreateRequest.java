package com.workflow.dto.asset;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetAssignmentCreateRequest {
    private Long assetId;
    private Long jobId; // optional if giving to a person without a job
    private Long assignedWorkerId; // optional
    private String notes;
}
