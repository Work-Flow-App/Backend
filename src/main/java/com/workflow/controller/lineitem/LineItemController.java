package com.workflow.controller.lineitem;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.estimate.LineItemCreateRequest;
import com.workflow.dto.estimate.LineItemResponse;
import com.workflow.dto.estimate.LineItemUpdateRequest;
import com.workflow.service.lineitem.ILineItemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Line Items")
@RestController
@RequestMapping("/api/v1/line-items")
@RequiredArgsConstructor
public class LineItemController {

    private final ILineItemService lineItemService;

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping
    public ResponseEntity<LineItemResponse> create(
            @Valid @RequestBody LineItemCreateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lineItemService.createLineItem(request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping
    public ResponseEntity<List<LineItemResponse>> getAll(Authentication auth) {
        return ResponseEntity.ok(lineItemService.getAllLineItems(getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{id}")
    public ResponseEntity<LineItemResponse> get(
            @PathVariable Long id,
            Authentication auth
    ) {
        return ResponseEntity.ok(lineItemService.getLineItem(id, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PutMapping("/{id}")
    public ResponseEntity<LineItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LineItemUpdateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(lineItemService.updateLineItem(id, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication auth
    ) {
        lineItemService.deleteLineItem(id, getCompanyId());
        return ResponseEntity.noContent().build();
    }

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }
}
