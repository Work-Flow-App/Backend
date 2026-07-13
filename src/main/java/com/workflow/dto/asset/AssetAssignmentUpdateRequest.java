package com.workflow.dto.asset;

import com.workflow.common.constant.asset.AssetLocationType;
import com.workflow.dto.job.AddressRequest;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetAssignmentUpdateRequest {
    private Long assignedWorkerId;
    private String notes;

    private AssetLocationType explicitLocationType; 
    private AddressRequest customAddress;
    private Integer expectedDurationDays;
}