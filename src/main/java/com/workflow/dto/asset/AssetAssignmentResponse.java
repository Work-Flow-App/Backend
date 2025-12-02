package com.workflow.dto.asset;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetAssignmentResponse {
    private Long assignmentId;
    private Long assetId;
    private Long jobId;
    private Long assignedWorkerId;
    private String notes;
    private LocalDateTime assignedAt;
    private LocalDateTime returnedAt;
    private Long durationDays;
    private String status; // ACTIVE or COMPLETED
}
