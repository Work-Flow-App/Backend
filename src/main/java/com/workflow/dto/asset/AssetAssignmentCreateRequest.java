package com.workflow.dto.asset;

import com.workflow.common.constant.asset.AssetLocationType;
import com.workflow.dto.job.AddressRequest;

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

    // Allows the user to manually override the location type (e.g., forcing WAREHOUSE)
    private AssetLocationType explicitLocationType; 
    
    // Allows passing a custom address, completely optional
    private AddressRequest customAddress;
}
