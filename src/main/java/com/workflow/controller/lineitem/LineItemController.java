package com.workflow.controller.lineitem;

import com.workflow.common.util.AuthUtils;
import com.workflow.dto.estimate.LineItemCreateRequest;
import com.workflow.dto.estimate.LineItemResponse;
import com.workflow.dto.estimate.LineItemUpdateRequest;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.lineitem.ILineItemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Line Items")
@RestController
@RequestMapping("/api/v1/line-items")
@RequiredArgsConstructor
public class LineItemController {

    private final ILineItemService lineItemService;
    private final ICompanyService companyService;

    @PostMapping
    public ResponseEntity<LineItemResponse> create(
            @Valid @RequestBody LineItemCreateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lineItemService.createLineItem(request, getCompanyId(auth)));
    }

    @GetMapping
    public ResponseEntity<List<LineItemResponse>> getAll(Authentication auth) {
        return ResponseEntity.ok(lineItemService.getAllLineItems(getCompanyId(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LineItemResponse> get(
            @PathVariable Long id,
            Authentication auth
    ) {
        return ResponseEntity.ok(lineItemService.getLineItem(id, getCompanyId(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LineItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LineItemUpdateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(lineItemService.updateLineItem(id, request, getCompanyId(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication auth
    ) {
        lineItemService.deleteLineItem(id, getCompanyId(auth));
        return ResponseEntity.noContent().build();
    }

    private Long getCompanyId(Authentication auth) {
        return AuthUtils.getCompanyId(auth, companyService);
    }
}
