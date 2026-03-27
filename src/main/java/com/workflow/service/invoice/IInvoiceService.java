package com.workflow.service.invoice;

import com.workflow.dto.invoice.InvoiceCreateRequest;
import com.workflow.dto.invoice.InvoiceResponse;

import java.util.List;

public interface IInvoiceService {
    InvoiceResponse generateInvoice(Long estimateId, InvoiceCreateRequest request, Long companyId);
    List<InvoiceResponse> getInvoicesForEstimate(Long estimateId, Long companyId);
    InvoiceResponse getInvoice(Long invoiceId, Long companyId);
}
