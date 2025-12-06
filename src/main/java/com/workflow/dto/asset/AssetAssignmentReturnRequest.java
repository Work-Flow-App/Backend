package com.workflow.dto.asset;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetAssignmentReturnRequest {
    private Long assignmentId;
    private String notes;
}
