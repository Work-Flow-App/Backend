package com.workflow.service.estimate;

import com.workflow.common.constant.financial.LineItemStatus;
import com.workflow.common.exception.business.*;
import com.workflow.common.util.LineItemCalculator;
import com.workflow.dto.estimate.*;
import com.workflow.entity.company.Company;
import com.workflow.entity.financial.Estimate;
import com.workflow.entity.financial.EstimateLineItem;
import com.workflow.entity.financial.LineItem;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.financial.EstimateLineItemRepository;
import com.workflow.repository.financial.EstimateRepository;
import com.workflow.repository.financial.LineItemRepository;
import com.workflow.service.estimatedocument.IEstimateDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class EstimateService implements IEstimateService {

    private final EstimateRepository estimateRepository;
    private final EstimateLineItemRepository estimateLineItemRepository;
    private final LineItemRepository lineItemRepository;
    private final CompanyRepository companyRepository;
    private final IEstimateDocumentService estimateDocumentService;

    @Override
    @Transactional(readOnly = true)
    public EstimateResponse getEstimate(Long estimateId, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));
        return toResponse(estimate);
    }

    @Override
    @Transactional(readOnly = true)
    public EstimateResponse getEstimateByJob(Long jobId, Long companyId) {
        Estimate estimate = estimateRepository.findByJobIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found for this job"));
        return toResponse(estimate);
    }

    @Override
    public EstimateResponse updateEstimate(Long estimateId, EstimateUpdateRequest request, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        estimate.setNotes(request.getNotes());
        estimateRepository.save(estimate);
        return toResponse(estimate);
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

        // Save to library
        LineItem libraryItem = LineItem.builder()
                .company(company)
                .productCode(request.getProductCode())
                .productDescription(request.getProductDescription())
                .additionalDetails(request.getAdditionalDetails())
                .unitPrice(request.getUnitPrice())
                .quantity(request.getQuantity())
                .vatRate(request.getVatRate())
                .build();
        LineItemCalculator.recalculate(libraryItem);
        lineItemRepository.save(libraryItem);

        // Create job-scoped copy
        EstimateLineItem estimateLineItem = EstimateLineItem.builder()
                .estimate(estimate)
                .sourceLineItemId(libraryItem.getId())
                .productCode(libraryItem.getProductCode())
                .productDescription(libraryItem.getProductDescription())
                .additionalDetails(libraryItem.getAdditionalDetails())
                .unitPrice(libraryItem.getUnitPrice())
                .quantity(libraryItem.getQuantity())
                .vatRate(libraryItem.getVatRate())
                .netAmount(libraryItem.getNetAmount())
                .vatAmount(libraryItem.getVatAmount())
                .totalAmount(libraryItem.getTotalAmount())
                .build();

        estimate.getLineItems().add(estimateLineItem);
        estimateRepository.save(estimate);

        return toResponse(estimate);
    }

    @Override
    public EstimateResponse linkExistingLineItem(Long estimateId, Long lineItemId, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        LineItem libraryItem = lineItemRepository.findByIdAndCompanyId(lineItemId, companyId)
                .orElseThrow(() -> new LineItemNotFoundException("Line item not found"));

        // Create isolated job-scoped copy
        EstimateLineItem estimateLineItem = EstimateLineItem.builder()
                .estimate(estimate)
                .sourceLineItemId(libraryItem.getId())
                .productCode(libraryItem.getProductCode())
                .productDescription(libraryItem.getProductDescription())
                .additionalDetails(libraryItem.getAdditionalDetails())
                .unitPrice(libraryItem.getUnitPrice())
                .quantity(libraryItem.getQuantity())
                .vatRate(libraryItem.getVatRate())
                .netAmount(libraryItem.getNetAmount())
                .vatAmount(libraryItem.getVatAmount())
                .totalAmount(libraryItem.getTotalAmount())
                .build();

        estimate.getLineItems().add(estimateLineItem);
        estimateRepository.save(estimate);

        return toResponse(estimate);
    }

    @Override
    public EstimateResponse updateEstimateLineItem(Long estimateId, Long estimateLineItemId,
                                                    LineItemUpdateRequest request, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        EstimateLineItem item = estimate.getLineItems().stream()
                .filter(eli -> eli.getId().equals(estimateLineItemId))
                .findFirst()
                .orElseThrow(() -> new LineItemNotFoundException("Line item not found on this estimate"));

        if (item.getStatus() == LineItemStatus.INVOICED) {
            throw new InvalidRequestException("Cannot edit an invoiced line item");
        }

        if (request.getProductCode() != null) item.setProductCode(request.getProductCode());
        if (request.getProductDescription() != null) item.setProductDescription(request.getProductDescription());
        if (request.getAdditionalDetails() != null) item.setAdditionalDetails(request.getAdditionalDetails());
        if (request.getUnitPrice() != null) item.setUnitPrice(request.getUnitPrice());
        if (request.getQuantity() != null) item.setQuantity(request.getQuantity());
        if (request.getVatRate() != null) item.setVatRate(request.getVatRate());

        LineItemCalculator.recalculate(item);

        boolean statusReverted = false;
        if (item.getStatus() == LineItemStatus.WAITING_APPROVAL
                || item.getStatus() == LineItemStatus.APPROVED) {
            item.setStatus(LineItemStatus.AVAILABLE);
            statusReverted = true;
        }

        estimateRepository.save(estimate);

        if (statusReverted) {
            estimateDocumentService.cleanupEmptyDocuments(estimateId, companyId);
        }

        return toResponse(estimate);
    }

    @Override
    public EstimateResponse unlinkLineItem(Long estimateId, Long estimateLineItemId, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        EstimateLineItem toRemove = estimate.getLineItems().stream()
                .filter(eli -> eli.getId().equals(estimateLineItemId))
                .findFirst()
                .orElseThrow(() -> new LineItemNotFoundException("Line item not found on this estimate"));

        if (toRemove.getStatus() == LineItemStatus.INVOICED) {
            throw new InvalidRequestException("Cannot remove an invoiced line item");
        }

        estimate.getLineItems().remove(toRemove);

        estimateRepository.save(estimate);

        estimateDocumentService.cleanupEmptyDocuments(estimateId, companyId);

        return toResponse(estimate);
    }

    @Override
    public EstimateResponse updateEstimateLineItemStatus(Long estimateId, Long estimateLineItemId,
                                                          LineItemStatusUpdateRequest request, Long companyId) {
        estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        EstimateLineItem item = estimateLineItemRepository
                .findByIdAndEstimateId(estimateLineItemId, estimateId)
                .orElseThrow(() -> new LineItemNotFoundException("Line item not found on this estimate"));

        if (item.getStatus() == LineItemStatus.INVOICED) {
            throw new InvalidRequestException("Cannot change status of an invoiced line item");
        }

        LineItemStatus requested = request.getStatus();
        boolean approve = requested == LineItemStatus.APPROVED
                && item.getStatus() == LineItemStatus.WAITING_APPROVAL;
        boolean reject = requested == LineItemStatus.AVAILABLE
                && item.getStatus() == LineItemStatus.WAITING_APPROVAL;

        if (!approve && !reject) {
            throw new InvalidRequestException(
                    "Line item must be in WAITING_APPROVAL status to be approved or rejected");
        }

        item.setStatus(requested);
        estimateLineItemRepository.save(item);

        if (reject) {
            estimateDocumentService.cleanupEmptyDocuments(estimateId, companyId);
        }

        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));
        return toResponse(estimate);
    }

    private EstimateResponse toResponse(Estimate estimate) {
        Set<Long> invoicedIds = estimate.getLineItems().stream()
                .filter(eli -> eli.getStatus() == LineItemStatus.INVOICED)
                .map(EstimateLineItem::getId)
                .collect(Collectors.toSet());
        return EstimateResponse.fromEntity(estimate, invoicedIds);
    }
}
