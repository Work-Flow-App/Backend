package com.workflow.controller.estimate;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.estimate.EstimateResponse;
import com.workflow.dto.estimate.EstimateUpdateRequest;
import com.workflow.dto.estimate.LineItemCreateRequest;
import com.workflow.dto.estimate.LineItemStatusUpdateRequest;
import com.workflow.dto.estimate.LineItemUpdateRequest;
import com.workflow.service.estimate.IEstimateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Estimates")
@RestController
@RequestMapping("/api/v1/estimates")
@RequiredArgsConstructor
public class EstimateController {

    private final IEstimateService estimateService;

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{id}")
    public ResponseEntity<EstimateResponse> get(
            @PathVariable Long id,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.getEstimate(id, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/job/{jobId}")
    public ResponseEntity<EstimateResponse> getByJob(
            @PathVariable Long jobId,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.getEstimateByJob(jobId, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PutMapping("/{id}")
    public ResponseEntity<EstimateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EstimateUpdateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.updateEstimate(id, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication auth
    ) {
        estimateService.deleteEstimate(id, getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping("/{estimateId}/line-items")
    public ResponseEntity<EstimateResponse> createAndLink(
            @PathVariable Long estimateId,
            @Valid @RequestBody LineItemCreateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estimateService.createAndLinkLineItem(estimateId, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PutMapping("/{estimateId}/line-items/{lineItemId}")
    public ResponseEntity<EstimateResponse> linkExisting(
            @PathVariable Long estimateId,
            @PathVariable Long lineItemId,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.linkExistingLineItem(estimateId, lineItemId, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PatchMapping("/{estimateId}/line-items/{estimateLineItemId}/status")
    public ResponseEntity<EstimateResponse> updateEstimateLineItemStatus(
            @PathVariable Long estimateId,
            @PathVariable Long estimateLineItemId,
            @Valid @RequestBody LineItemStatusUpdateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.updateEstimateLineItemStatus(
                estimateId, estimateLineItemId, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PatchMapping("/{estimateId}/line-items/{estimateLineItemId}")
    public ResponseEntity<EstimateResponse> updateEstimateLineItem(
            @PathVariable Long estimateId,
            @PathVariable Long estimateLineItemId,
            @Valid @RequestBody LineItemUpdateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.updateEstimateLineItem(
                estimateId, estimateLineItemId, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/{estimateId}/line-items/{estimateLineItemId}")
    public ResponseEntity<EstimateResponse> unlink(
            @PathVariable Long estimateId,
            @PathVariable Long estimateLineItemId,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.unlinkLineItem(estimateId, estimateLineItemId, getCompanyId()));
    }

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }
}
