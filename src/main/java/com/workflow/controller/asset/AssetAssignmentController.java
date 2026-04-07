package com.workflow.controller.asset;

import com.workflow.common.util.AuthUtils;
import com.workflow.dto.asset.*;
import com.workflow.service.asset.IAssetAssignmentService;
import com.workflow.service.company.ICompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Asset Assignments")
@RestController
@RequestMapping("/api/v1/asset-assignments")
@RequiredArgsConstructor
public class AssetAssignmentController {

    private final IAssetAssignmentService assignmentService;
    private final ICompanyService companyService;

    private Long getCompanyId(Authentication auth) {
        return AuthUtils.getCompanyId(auth, companyService);
    }

    @PostMapping("/assign")
    public ResponseEntity<AssetAssignmentResponse> assign(@Valid @RequestBody AssetAssignmentCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.assignAsset(request, getCompanyId(auth)));
    }

    @PostMapping("/return")
    public ResponseEntity<AssetAssignmentResponse> returnAsset(@Valid @RequestBody AssetAssignmentReturnRequest request,
            Authentication auth) {
        return ResponseEntity.ok(assignmentService.returnAsset(request, getCompanyId(auth)));
    }

    @GetMapping("/asset/{assetId}/history")
    public ResponseEntity<List<AssetAssignmentResponse>> history(@PathVariable Long assetId, Authentication auth) {
        return ResponseEntity.ok(assignmentService.getAssignmentHistory(assetId, getCompanyId(auth)));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<AssetAssignmentResponse>> jobAssignments(@PathVariable Long jobId,
            @RequestParam(defaultValue = "true") boolean onlyActive,
            Authentication auth) {
        return ResponseEntity.ok(assignmentService.getAssignedAssetsForJob(jobId, getCompanyId(auth), onlyActive));
    }
}
