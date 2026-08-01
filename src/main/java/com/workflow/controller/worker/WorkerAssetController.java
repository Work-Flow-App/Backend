package com.workflow.controller.worker;

import com.workflow.dto.asset.AssetAssignmentResponse;
import com.workflow.dto.asset.AssetResponse;
import com.workflow.dto.job.AddressRequest;
import com.workflow.entity.auth.User;
import com.workflow.service.asset.IWorkerAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Worker Assets", description = "Endpoints for workers to view and manage their assigned assets")
@RestController
@RequestMapping("/api/v1/worker/assets")
@RequiredArgsConstructor
public class WorkerAssetController {

    private final IWorkerAssetService workerAssetService;

    private Long getUserId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return user.getId();
    }

    @Operation(summary = "Get all active assets assigned to the current worker")
    @GetMapping
    public ResponseEntity<List<AssetAssignmentResponse>> getMyAssignedAssets(Authentication auth) {
        return ResponseEntity.ok(workerAssetService.getMyAssignedAssets(getUserId(auth)));
    }

    @Operation(summary = "Get full details of a specific assigned asset")
    @GetMapping("/{assetId}")
    public ResponseEntity<AssetResponse> getAssignedAssetDetails(
            @PathVariable Long assetId,
            Authentication auth) {
        return ResponseEntity.ok(workerAssetService.getAssignedAssetDetails(assetId, getUserId(auth)));
    }

    @Operation(summary = "Update the physical address of an assigned asset")
    @PutMapping("/assignments/{assignmentId}/address")
    public ResponseEntity<AssetAssignmentResponse> updateAssetAddress(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AddressRequest request,
            Authentication auth) {
        return ResponseEntity.ok(workerAssetService.updateAssetAddress(assignmentId, request, getUserId(auth)));
    }
}