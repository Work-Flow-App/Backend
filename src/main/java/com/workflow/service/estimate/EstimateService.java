package com.workflow.service.estimate;

import com.workflow.common.exception.business.*;
import com.workflow.common.util.LineItemCalculator;
import com.workflow.dto.estimate.*;
import com.workflow.entity.company.Company;
import com.workflow.entity.financial.Estimate;
import com.workflow.entity.financial.LineItem;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.financial.EstimateRepository;
import com.workflow.repository.financial.LineItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EstimateService implements IEstimateService {

    private final EstimateRepository estimateRepository;
    private final LineItemRepository lineItemRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public EstimateResponse getEstimate(Long estimateId, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));
        return EstimateResponse.fromEntity(estimate);
    }

    @Override
    @Transactional(readOnly = true)
    public EstimateResponse getEstimateByJob(Long jobId, Long companyId) {
        Estimate estimate = estimateRepository.findByJobIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found for this job"));
        return EstimateResponse.fromEntity(estimate);
    }

    @Override
    public EstimateResponse updateEstimate(Long estimateId, EstimateUpdateRequest request, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        estimate.setNotes(request.getNotes());
        estimateRepository.save(estimate);
        return EstimateResponse.fromEntity(estimate);
    }

    @Override
    public void deleteEstimate(Long estimateId, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));
        estimateRepository.delete(estimate);
    }

    @Override
    public EstimateResponse createAndLinkLineItem(Long estimateId, LineItemCreateRequest request, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));

        LineItem item = LineItem.builder()
                .company(company)
                .productCode(request.getProductCode())
                .productDescription(request.getProductDescription())
                .additionalDetails(request.getAdditionalDetails())
                .unitPrice(request.getUnitPrice())
                .coreOrSub(request.getCoreOrSub())
                .quantity(request.getQuantity())
                .vatRate(request.getVatRate())
                .build();

        LineItemCalculator.recalculate(item);
        lineItemRepository.save(item);
        estimate.getLineItems().add(item);
        estimateRepository.save(estimate);

        return EstimateResponse.fromEntity(estimate);
    }

    @Override
    public EstimateResponse linkExistingLineItem(Long estimateId, Long lineItemId, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        LineItem lineItem = lineItemRepository.findByIdAndCompanyId(lineItemId, companyId)
                .orElseThrow(() -> new LineItemNotFoundException("Line item not found"));

        boolean alreadyLinked = estimate.getLineItems().stream()
                .anyMatch(l -> l.getId().equals(lineItemId));

        if (!alreadyLinked) {
            estimate.getLineItems().add(lineItem);
            estimateRepository.save(estimate);
        }

        return EstimateResponse.fromEntity(estimate);
    }

    @Override
    public EstimateResponse unlinkLineItem(Long estimateId, Long lineItemId, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        boolean removed = estimate.getLineItems().removeIf(l -> l.getId().equals(lineItemId));

        if (!removed) {
            throw new LineItemNotFoundException("Line item not linked to this estimate");
        }

        estimateRepository.save(estimate);
        return EstimateResponse.fromEntity(estimate);
    }

}
