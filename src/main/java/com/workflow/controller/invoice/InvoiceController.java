package com.workflow.controller.invoice;

import com.workflow.dto.invoice.InvoiceCreateRequest;
import com.workflow.dto.invoice.InvoiceResponse;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.invoice.IInvoiceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Invoices")
@RestController
@RequestMapping("/api/v1/estimates")
@RequiredArgsConstructor
public class InvoiceController {

    private final IInvoiceService invoiceService;
    private final ICompanyService companyService;

    @PostMapping("/{estimateId}/invoice")
    public ResponseEntity<InvoiceResponse> generate(
            @PathVariable Long estimateId,
            @Valid @RequestBody InvoiceCreateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.generateInvoice(estimateId, request, getCompanyId(auth)));
    }

    @GetMapping("/{estimateId}/invoices")
    public ResponseEntity<List<InvoiceResponse>> listForEstimate(
            @PathVariable Long estimateId,
            Authentication auth
    ) {
        return ResponseEntity.ok(invoiceService.getInvoicesForEstimate(estimateId, getCompanyId(auth)));
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<InvoiceResponse> get(
            @PathVariable Long invoiceId,
            Authentication auth
    ) {
        return ResponseEntity.ok(invoiceService.getInvoice(invoiceId, getCompanyId(auth)));
    }

    private Long getCompanyId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        Company company = companyService.findCompanyByUserId(user.getId());
        return company.getId();
    }
}
