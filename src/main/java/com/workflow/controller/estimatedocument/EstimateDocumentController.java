package com.workflow.controller.estimatedocument;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.estimatedocument.EstimateDocumentCreateRequest;
import com.workflow.dto.estimatedocument.EstimateDocumentResponse;
import com.workflow.service.estimatedocument.IEstimateDocumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Estimate Documents")
@RestController
@RequestMapping("/api/v1/estimates")
@RequiredArgsConstructor
public class EstimateDocumentController {

    private final IEstimateDocumentService estimateDocumentService;

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PostMapping("/{estimateId}/documents")
    public ResponseEntity<EstimateDocumentResponse> generate(
            @PathVariable Long estimateId,
            @Valid @RequestBody EstimateDocumentCreateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estimateDocumentService.generateEstimateDocument(estimateId, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{estimateId}/documents")
    public ResponseEntity<List<EstimateDocumentResponse>> listForEstimate(
            @PathVariable Long estimateId,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateDocumentService.getEstimateDocuments(estimateId, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<EstimateDocumentResponse> get(
            @PathVariable Long documentId,
            Authentication auth
    ) {
        return ResponseEntity.ok(estimateDocumentService.getEstimateDocument(documentId, getCompanyId()));
    }

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }
}
