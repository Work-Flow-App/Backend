package com.workflow.service.dashboard;

import com.workflow.dto.dashboard.FinancialSummaryResponse;
import com.workflow.repository.financial.EstimateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService implements IDashboardService {

    private final EstimateRepository estimateRepository;

    @Override
    public FinancialSummaryResponse getFinancialSummary(Long companyId) {
        BigDecimal waitingApproval = estimateRepository.sumWaitingApprovalByCompanyId(companyId);
        BigDecimal approved = estimateRepository.sumApprovedByCompanyId(companyId);
        BigDecimal invoiced = estimateRepository.sumInvoicedByCompanyId(companyId);
        BigDecimal allTimeInvoiced = estimateRepository.sumAllTimeInvoicedByCompanyId(companyId);

        return FinancialSummaryResponse.builder()
                .waitingApprovalValue(waitingApproval)
                .approvedValue(approved)
                .invoicedValue(invoiced)
                .allTimeInvoicedValue(allTimeInvoiced)
                .build();
    }
}
