package com.workflow.controller.asset;

import com.workflow.dto.asset.*;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.service.asset.IAssetAssignmentService;
import com.workflow.service.company.ICompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/asset-assignments")
@RequiredArgsConstructor
public class AssetAssignmentController {

    private final IAssetAssignmentService assignmentService;
    private final ICompanyService companyService;

    private Long getCompanyId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        Company company = companyService.findCompanyByUserId(user.getId());
        return company.getId();
    }

    @PostMapping("/assign")
    public ResponseEntity<AssetAssignmentResponse> assign(@RequestBody AssetAssignmentCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.assignAsset(request, getCompanyId(auth)));
    }

    @PostMapping("/return")
    public ResponseEntity<AssetAssignmentResponse> returnAsset(@RequestBody AssetAssignmentReturnRequest request,
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
