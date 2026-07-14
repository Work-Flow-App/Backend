package com.workflow.controller.asset;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.asset.*;
import com.workflow.service.asset.IAssetAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Asset Assignments")
@RestController
@RequestMapping("/api/v1/asset-assignments")
@RequiredArgsConstructor
public class AssetAssignmentController {

    private final IAssetAssignmentService assignmentService;

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PostMapping("/assign")
    public ResponseEntity<AssetAssignmentResponse> assign(@Valid @RequestBody AssetAssignmentCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.assignAsset(request, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PostMapping("/return")
    public ResponseEntity<AssetAssignmentResponse> returnAsset(@Valid @RequestBody AssetAssignmentReturnRequest request,
            Authentication auth) {
        return ResponseEntity.ok(assignmentService.returnAsset(request, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping("/asset/{assetId}/history")
    public ResponseEntity<List<AssetAssignmentResponse>> history(@PathVariable Long assetId, Authentication auth) {
        return ResponseEntity.ok(assignmentService.getAssignmentHistory(assetId, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PutMapping("/assign/{assignmentId}")
    public ResponseEntity<AssetAssignmentResponse> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssetAssignmentUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(assignmentService.updateAssignment(assignmentId, request, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<AssetAssignmentResponse>> jobAssignments(@PathVariable Long jobId,
            @RequestParam(defaultValue = "true") boolean onlyActive,
            Authentication auth) {
        return ResponseEntity.ok(assignmentService.getAssignedAssetsForJob(jobId, getCompanyId(), onlyActive));
    }
}
