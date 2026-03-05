package com.workflow.service.lineitem;

import com.workflow.common.constant.CoreOrSub;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.LineItemNotFoundException;
import com.workflow.dto.estimate.LineItemCreateRequest;
import com.workflow.dto.estimate.LineItemResponse;
import com.workflow.dto.estimate.LineItemUpdateRequest;
import com.workflow.entity.Company;
import com.workflow.entity.LineItem;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.LineItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LineItemServiceTest {

    @Mock private LineItemRepository lineItemRepository;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private LineItemService lineItemService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = Company.builder().id(1L).name("Test Company").build();
    }

    // ============= createLineItem — pricing =============

    @Test
    void createLineItem_ShouldCalculateNetVatTotalCorrectly() {
        // 50.00 × 2 = 100.00 net; 100.00 × 0.19 = 19.00 vat; total = 119.00
        LineItemCreateRequest request = LineItemCreateRequest.builder()
                .productCode("P001").productDescription("Labour")
                .unitPrice(new BigDecimal("50.00")).coreOrSub(CoreOrSub.CORE)
                .quantity(new BigDecimal("2.0000")).vatRate(new BigDecimal("0.1900"))
                .build();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(lineItemRepository.save(any())).thenAnswer(inv -> { LineItem i = inv.getArgument(0); i.setId(1L); return i; });

        LineItemResponse response = lineItemService.createLineItem(request, 1L);

        assertThat(response.getNetAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getVatAmount()).isEqualByComparingTo("19.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("119.00");
        assertThat(response.getProductCode()).isEqualTo("P001");
        assertThat(response.getCoreOrSub()).isEqualTo(CoreOrSub.CORE);
    }

    @Test
    void createLineItem_ShouldRoundHalfUp() {
        // 10.00 × 3.3333 = 33.333 → rounds to 33.33; 33.33 × 0.20 = 6.666 → rounds to 6.67
        LineItemCreateRequest request = LineItemCreateRequest.builder()
                .productCode("P002").productDescription("Materials")
                .unitPrice(new BigDecimal("10.00")).coreOrSub(CoreOrSub.SUB)
                .quantity(new BigDecimal("3.3333")).vatRate(new BigDecimal("0.2000"))
                .build();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(lineItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LineItemResponse response = lineItemService.createLineItem(request, 1L);

        assertThat(response.getNetAmount()).isEqualByComparingTo("33.33");
        assertThat(response.getVatAmount()).isEqualByComparingTo("6.67");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    void createLineItem_ShouldThrowNotFound_WhenCompanyNotFound() {
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> lineItemService.createLineItem(
                LineItemCreateRequest.builder().productCode("P").productDescription("D")
                        .unitPrice(BigDecimal.TEN).coreOrSub(CoreOrSub.CORE)
                        .quantity(BigDecimal.ONE).vatRate(new BigDecimal("0.19")).build(), 1L))
                .isInstanceOf(CompanyNotFoundException.class);
        verify(lineItemRepository, never()).save(any());
    }

    // ============= getLineItem =============

    @Test
    void getLineItem_ShouldReturnItem() {
        LineItem item = buildItem(1L);
        when(lineItemRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(item));

        LineItemResponse response = lineItemService.getLineItem(1L, 1L);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getProductCode()).isEqualTo("P001");
    }

    @Test
    void getLineItem_ShouldThrowNotFound() {
        when(lineItemRepository.findByIdAndCompanyId(999L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> lineItemService.getLineItem(999L, 1L))
                .isInstanceOf(LineItemNotFoundException.class);
    }

    // ============= getAllLineItems =============

    @Test
    void getAllLineItems_ShouldReturnAllForCompany() {
        when(lineItemRepository.findByCompanyId(1L)).thenReturn(List.of(buildItem(1L), buildItem(2L)));
        assertThat(lineItemService.getAllLineItems(1L)).hasSize(2);
    }

    // ============= updateLineItem =============

    @Test
    void updateLineItem_ShouldUpdateFieldsAndRecalculate() {
        LineItem item = buildItem(1L);
        when(lineItemRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(item));
        when(lineItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Change unit price to 100, quantity to 3 → net=300, vat=57, total=357
        LineItemUpdateRequest request = LineItemUpdateRequest.builder()
                .unitPrice(new BigDecimal("100.00"))
                .quantity(new BigDecimal("3.0000"))
                .build();

        LineItemResponse response = lineItemService.updateLineItem(1L, request, 1L);

        assertThat(response.getNetAmount()).isEqualByComparingTo("300.00");
        assertThat(response.getVatAmount()).isEqualByComparingTo("57.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("357.00");
    }

    @Test
    void updateLineItem_ShouldOnlyUpdateProvidedFields() {
        LineItem item = buildItem(1L);
        when(lineItemRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(item));
        when(lineItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Only update productCode
        LineItemUpdateRequest request = LineItemUpdateRequest.builder().productCode("P-UPDATED").build();
        LineItemResponse response = lineItemService.updateLineItem(1L, request, 1L);

        assertThat(response.getProductCode()).isEqualTo("P-UPDATED");
        assertThat(response.getProductDescription()).isEqualTo("Labour"); // unchanged
    }

    @Test
    void updateLineItem_ShouldThrowNotFound() {
        when(lineItemRepository.findByIdAndCompanyId(999L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> lineItemService.updateLineItem(999L,
                LineItemUpdateRequest.builder().productCode("P").build(), 1L))
                .isInstanceOf(LineItemNotFoundException.class);
    }

    // ============= deleteLineItem =============

    @Test
    void deleteLineItem_ShouldDeleteSuccessfully() {
        LineItem item = buildItem(1L);
        when(lineItemRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(item));

        lineItemService.deleteLineItem(1L, 1L);
        verify(lineItemRepository).delete(item);
    }

    @Test
    void deleteLineItem_ShouldThrowNotFound() {
        when(lineItemRepository.findByIdAndCompanyId(999L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> lineItemService.deleteLineItem(999L, 1L))
                .isInstanceOf(LineItemNotFoundException.class);
        verify(lineItemRepository, never()).delete(any());
    }

    // ============= helper =============

    private LineItem buildItem(Long id) {
        return LineItem.builder()
                .id(id).company(company)
                .productCode("P001").productDescription("Labour")
                .unitPrice(new BigDecimal("50.00")).coreOrSub(CoreOrSub.CORE)
                .quantity(new BigDecimal("2.0000")).vatRate(new BigDecimal("0.1900"))
                .netAmount(new BigDecimal("100.00"))
                .vatAmount(new BigDecimal("19.00"))
                .totalAmount(new BigDecimal("119.00"))
                .build();
    }
}
