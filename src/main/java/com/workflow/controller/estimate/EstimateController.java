package com.workflow.controller.estimate;

import com.workflow.common.util.AuthUtils;
import com.workflow.dto.estimate.EstimateResponse;
import com.workflow.dto.estimate.EstimateUpdateRequest;
import com.workflow.dto.estimate.LineItemCreateRequest;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.estimate.IEstimateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Estimates")
@RestController
@RequestMapping("/api/v1/estimates")
@RequiredArgsConstructor
public class EstimateController {

    private final IEstimateService estimateService;
    private final ICompanyService companyService;

    @GetMapping("/{id}")
    public ResponseEntity<EstimateResponse> get(
            @PathVariable Long id,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.getEstimate(id, getCompanyId(auth)));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<EstimateResponse> getByJob(
            @PathVariable Long jobId,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.getEstimateByJob(jobId, getCompanyId(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstimateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EstimateUpdateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.updateEstimate(id, request, getCompanyId(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication auth
    ) {
        estimateService.deleteEstimate(id, getCompanyId(auth));
        return ResponseEntity.noContent().build();
    }

    /** Create a brand-new line item and immediately attach it to this estimate. */
    @PostMapping("/{estimateId}/line-items")
    public ResponseEntity<EstimateResponse> createAndLink(
            @PathVariable Long estimateId,
            @Valid @RequestBody LineItemCreateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estimateService.createAndLinkLineItem(estimateId, request, getCompanyId(auth)));
    }

    /** Attach an existing line item (by ID) to this estimate. */
    @PutMapping("/{estimateId}/line-items/{lineItemId}")
    public ResponseEntity<EstimateResponse> linkExisting(
            @PathVariable Long estimateId,
            @PathVariable Long lineItemId,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.linkExistingLineItem(estimateId, lineItemId, getCompanyId(auth)));
    }

    /** Remove a line item from this estimate (line item itself is NOT deleted). */
    @DeleteMapping("/{estimateId}/line-items/{lineItemId}")
    public ResponseEntity<EstimateResponse> unlink(
            @PathVariable Long estimateId,
            @PathVariable Long lineItemId,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateService.unlinkLineItem(estimateId, lineItemId, getCompanyId(auth)));
    }

    private Long getCompanyId(Authentication auth) {
        return AuthUtils.getCompanyId(auth, companyService);
    }
}
