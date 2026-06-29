package com.workflow.service.asset;

import com.workflow.dto.asset.*;
import java.util.List;

public interface IAssetAssignmentService {
    AssetAssignmentResponse assignAsset(AssetAssignmentCreateRequest request, Long companyId);

    AssetAssignmentResponse returnAsset(AssetAssignmentReturnRequest request, Long companyId);

    AssetAssignmentResponse updateAssignment(Long assignmentId, AssetAssignmentUpdateRequest request, Long companyId);

    void syncJobAssets(Long jobId, List<Long> assetIds, Long companyId);
    
    List<AssetAssignmentResponse> getAssignmentHistory(Long assetId, Long companyId);

    List<AssetAssignmentResponse> getAssignedAssetsForJob(Long jobId, Long companyId, boolean onlyActive);
}
