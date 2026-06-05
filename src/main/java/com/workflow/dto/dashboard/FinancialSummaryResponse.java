package com.workflow.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FinancialSummaryResponse {
    private BigDecimal waitingApprovalValue;
    private BigDecimal approvedValue;
    private BigDecimal invoicedValue;
    private BigDecimal allTimeInvoicedValue;
}
