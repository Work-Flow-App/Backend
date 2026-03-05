package com.workflow.service.lineitem;

import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.LineItemNotFoundException;
import com.workflow.dto.estimate.LineItemCreateRequest;
import com.workflow.dto.estimate.LineItemResponse;
import com.workflow.dto.estimate.LineItemUpdateRequest;
import com.workflow.entity.Company;
import com.workflow.entity.LineItem;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.LineItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LineItemService implements ILineItemService {

    private final LineItemRepository lineItemRepository;
    private final CompanyRepository companyRepository;

    @Override
    public LineItemResponse createLineItem(LineItemCreateRequest request, Long companyId) {
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

        calculateAmounts(item);
        lineItemRepository.save(item);
        return LineItemResponse.fromEntity(item);
    }

    @Override
    @Transactional(readOnly = true)
    public LineItemResponse getLineItem(Long id, Long companyId) {
        LineItem item = lineItemRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new LineItemNotFoundException("Line item not found"));
        return LineItemResponse.fromEntity(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LineItemResponse> getAllLineItems(Long companyId) {
        return lineItemRepository.findByCompanyId(companyId).stream()
                .map(LineItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public LineItemResponse updateLineItem(Long id, LineItemUpdateRequest request, Long companyId) {
        LineItem item = lineItemRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new LineItemNotFoundException("Line item not found"));

        if (request.getProductCode() != null) item.setProductCode(request.getProductCode());
        if (request.getProductDescription() != null) item.setProductDescription(request.getProductDescription());
        if (request.getAdditionalDetails() != null) item.setAdditionalDetails(request.getAdditionalDetails());
        if (request.getUnitPrice() != null) item.setUnitPrice(request.getUnitPrice());
        if (request.getCoreOrSub() != null) item.setCoreOrSub(request.getCoreOrSub());
        if (request.getQuantity() != null) item.setQuantity(request.getQuantity());
        if (request.getVatRate() != null) item.setVatRate(request.getVatRate());

        calculateAmounts(item);
        lineItemRepository.save(item);
        return LineItemResponse.fromEntity(item);
    }

    @Override
    public void deleteLineItem(Long id, Long companyId) {
        LineItem item = lineItemRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new LineItemNotFoundException("Line item not found"));
        lineItemRepository.delete(item);
    }

    private void calculateAmounts(LineItem item) {
        BigDecimal netAmount = item.getUnitPrice()
                .multiply(item.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal vatAmount = netAmount
                .multiply(item.getVatRate())
                .setScale(2, RoundingMode.HALF_UP);

        item.setNetAmount(netAmount);
        item.setVatAmount(vatAmount);
        item.setTotalAmount(netAmount.add(vatAmount));
    }
}
