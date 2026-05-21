package com.workflow.controller.invoice;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.invoice.InvoiceCreateRequest;
import com.workflow.dto.invoice.InvoiceResponse;
import com.workflow.service.invoice.IInvoiceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Invoices")
@RestController
@RequestMapping("/api/v1/estimates")
@RequiredArgsConstructor
public class InvoiceController {

    private final IInvoiceService invoiceService;

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> listAll(Authentication auth) {
        return ResponseEntity.ok(invoiceService.getAllInvoices(getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping("/{estimateId}/invoice")
    public ResponseEntity<InvoiceResponse> generate(
            @PathVariable Long estimateId,
            @Valid @RequestBody InvoiceCreateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.generateInvoice(estimateId, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{estimateId}/invoices")
    public ResponseEntity<List<InvoiceResponse>> listForEstimate(
            @PathVariable Long estimateId,
            Authentication auth
    ) {
        return ResponseEntity.ok(invoiceService.getInvoicesForEstimate(estimateId, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<InvoiceResponse> get(
            @PathVariable Long invoiceId,
            Authentication auth
    ) {
        return ResponseEntity.ok(invoiceService.getInvoice(invoiceId, getCompanyId()));
    }

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }
}
