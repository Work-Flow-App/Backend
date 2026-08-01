package com.workflow.service.asset;

import com.workflow.dto.asset.AssetAssignmentResponse;
import com.workflow.dto.asset.AssetResponse;
import com.workflow.dto.job.AddressRequest;

import java.util.List;

public interface IWorkerAssetService {

    /**
     * Get all active asset assignments for the current worker.
     */
    List<AssetAssignmentResponse> getMyAssignedAssets(Long workerUserId);

    /**
     * Get details of a specific asset, only if currently assigned to the worker.
     */
    AssetResponse getAssignedAssetDetails(Long assetId, Long workerUserId);

    /**
     * Update the physical location/address of an asset currently assigned to the
     * worker.
     */
    AssetAssignmentResponse updateAssetAddress(Long assignmentId, AddressRequest addressRequest, Long workerUserId);
}