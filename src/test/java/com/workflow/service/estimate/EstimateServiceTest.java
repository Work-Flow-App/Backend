package com.workflow.service.estimate;

import com.workflow.common.constant.CoreOrSub;
import com.workflow.common.exception.business.*;
import com.workflow.dto.estimate.*;
import com.workflow.entity.*;
import com.workflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstimateServiceTest {

    @Mock private EstimateRepository estimateRepository;
    @Mock private LineItemRepository lineItemRepository;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private EstimateService estimateService;

    private Company company;
    private Job job;
    private Estimate estimate;

    @BeforeEach
    void setUp() {
        company = Company.builder().id(1L).name("Test Company").build();
        job = Job.builder().id(10L).company(company).build();
        estimate = Estimate.builder()
                .id(100L).job(job).company(company).notes("Notes")
                .lineItems(new ArrayList<>()).build();
    }

    // ============= getEstimate / getEstimateByJob =============

    @Test
    void getEstimate_ShouldReturnEstimate() {
        when(estimateRepository.findByIdAndCompanyId(100L, 1L)).thenReturn(Optional.of(estimate));
        assertThat(estimateService.getEstimate(100L, 1L).getId()).isEqualTo(100L);
    }

    @Test
    void getEstimate_ShouldThrowNotFound() {
        when(estimateRepository.findByIdAndCompanyId(999L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> estimateService.getEstimate(999L, 1L))
                .isInstanceOf(EstimateNotFoundException.class);
    }

    @Test
    void getEstimateByJob_ShouldReturnEstimate() {
        when(estimateRepository.findByJobIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(estimate));
        assertThat(estimateService.getEstimateByJob(10L, 1L).getJobId()).isEqualTo(10L);
    }

    // ============= updateEstimate =============

    @Test
    void updateEstimate_ShouldUpdateNotes() {
        when(estimateRepository.findByIdAndCompanyId(100L, 1L)).thenReturn(Optional.of(estimate));
        when(estimateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EstimateResponse response = estimateService.updateEstimate(100L,
                EstimateUpdateRequest.builder().notes("Updated").build(), 1L);
        assertThat(response.getNotes()).isEqualTo("Updated");
    }

    // ============= deleteEstimate =============

    @Test
    void deleteEstimate_ShouldDelete() {
        when(estimateRepository.findByIdAndCompanyId(100L, 1L)).thenReturn(Optional.of(estimate));
        estimateService.deleteEstimate(100L, 1L);
        verify(estimateRepository).delete(estimate);
    }

    // ============= createAndLinkLineItem =============

    @Test
    void createAndLinkLineItem_ShouldCalculateAndLink() {
        // net = 50 × 2 = 100, vat = 100 × 0.19 = 19, total = 119
        LineItemCreateRequest request = LineItemCreateRequest.builder()
                .productCode("P001").productDescription("Labour")
                .unitPrice(new BigDecimal("50.00")).coreOrSub(CoreOrSub.CORE)
                .quantity(new BigDecimal("2.0000")).vatRate(new BigDecimal("0.1900"))
                .build();

        when(estimateRepository.findByIdAndCompanyId(100L, 1L)).thenReturn(Optional.of(estimate));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(lineItemRepository.save(any())).thenAnswer(inv -> { LineItem i = inv.getArgument(0); i.setId(200L); return i; });
        when(estimateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EstimateResponse response = estimateService.createAndLinkLineItem(100L, request, 1L);

        assertThat(response.getLineItems()).hasSize(1);
        LineItemResponse item = response.getLineItems().get(0);
        assertThat(item.getNetAmount()).isEqualByComparingTo("100.00");
        assertThat(item.getVatAmount()).isEqualByComparingTo("19.00");
        assertThat(item.getTotalAmount()).isEqualByComparingTo("119.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("119.00");
        verify(lineItemRepository).save(any(LineItem.class));
    }

    @Test
    void createAndLinkLineItem_ShouldThrowNotFound_WhenEstimateNotFound() {
        when(estimateRepository.findByIdAndCompanyId(999L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> estimateService.createAndLinkLineItem(999L,
                LineItemCreateRequest.builder().productCode("P").productDescription("D")
                        .unitPrice(BigDecimal.TEN).coreOrSub(CoreOrSub.CORE)
                        .quantity(BigDecimal.ONE).vatRate(new BigDecimal("0.19")).build(), 1L))
                .isInstanceOf(EstimateNotFoundException.class);
        verify(lineItemRepository, never()).save(any());
    }

    // ============= linkExistingLineItem =============

    @Test
    void linkExistingLineItem_ShouldLinkSuccessfully() {
        LineItem lineItem = LineItem.builder().id(200L).company(company)
                .netAmount(new BigDecimal("100.00")).vatAmount(new BigDecimal("19.00"))
                .totalAmount(new BigDecimal("119.00")).build();

        when(estimateRepository.findByIdAndCompanyId(100L, 1L)).thenReturn(Optional.of(estimate));
        when(lineItemRepository.findByIdAndCompanyId(200L, 1L)).thenReturn(Optional.of(lineItem));
        when(estimateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EstimateResponse response = estimateService.linkExistingLineItem(100L, 200L, 1L);

        assertThat(response.getLineItems()).hasSize(1);
        assertThat(response.getGrandTotal()).isEqualByComparingTo("119.00");
    }

    @Test
    void linkExistingLineItem_ShouldBeIdempotent_WhenAlreadyLinked() {
        LineItem lineItem = LineItem.builder().id(200L).company(company)
                .netAmount(BigDecimal.ZERO).vatAmount(BigDecimal.ZERO).totalAmount(BigDecimal.ZERO).build();
        estimate.getLineItems().add(lineItem); // already linked

        when(estimateRepository.findByIdAndCompanyId(100L, 1L)).thenReturn(Optional.of(estimate));
        when(lineItemRepository.findByIdAndCompanyId(200L, 1L)).thenReturn(Optional.of(lineItem));

        EstimateResponse response = estimateService.linkExistingLineItem(100L, 200L, 1L);

        assertThat(response.getLineItems()).hasSize(1); // not duplicated
        verify(estimateRepository, never()).save(any()); // no redundant save
    }

    @Test
    void linkExistingLineItem_ShouldThrowNotFound_WhenLineItemBelongsToDifferentCompany() {
        when(estimateRepository.findByIdAndCompanyId(100L, 1L)).thenReturn(Optional.of(estimate));
        when(lineItemRepository.findByIdAndCompanyId(200L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estimateService.linkExistingLineItem(100L, 200L, 1L))
                .isInstanceOf(LineItemNotFoundException.class);
    }

    // ============= unlinkLineItem =============

    @Test
    void unlinkLineItem_ShouldRemoveFromEstimateOnly() {
        LineItem lineItem = LineItem.builder().id(200L).company(company)
                .netAmount(new BigDecimal("100.00")).vatAmount(new BigDecimal("19.00"))
                .totalAmount(new BigDecimal("119.00")).build();
        estimate.getLineItems().add(lineItem);

        when(estimateRepository.findByIdAndCompanyId(100L, 1L)).thenReturn(Optional.of(estimate));
        when(estimateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EstimateResponse response = estimateService.unlinkLineItem(100L, 200L, 1L);

        assertThat(response.getLineItems()).isEmpty();
        assertThat(response.getGrandTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        // Line item itself must NOT be deleted
        verify(lineItemRepository, never()).delete(any());
    }

    @Test
    void unlinkLineItem_ShouldThrowNotFound_WhenLineItemNotLinked() {
        when(estimateRepository.findByIdAndCompanyId(100L, 1L)).thenReturn(Optional.of(estimate));

        assertThatThrownBy(() -> estimateService.unlinkLineItem(100L, 999L, 1L))
                .isInstanceOf(LineItemNotFoundException.class)
                .hasMessageContaining("not linked");
    }

    // ============= Aggregate totals =============

    @Test
    void aggregateTotals_ShouldSumAcrossAllLinkedItems() {
        LineItem item1 = LineItem.builder().id(1L).company(company)
                .netAmount(new BigDecimal("100.00")).vatAmount(new BigDecimal("19.00"))
                .totalAmount(new BigDecimal("119.00")).build();
        LineItem item2 = LineItem.builder().id(2L).company(company)
                .netAmount(new BigDecimal("200.00")).vatAmount(new BigDecimal("38.00"))
                .totalAmount(new BigDecimal("238.00")).build();
        estimate.getLineItems().add(item1);
        estimate.getLineItems().add(item2);

        when(estimateRepository.findByIdAndCompanyId(100L, 1L)).thenReturn(Optional.of(estimate));

        EstimateResponse response = estimateService.getEstimate(100L, 1L);

        assertThat(response.getTotalNet()).isEqualByComparingTo("300.00");
        assertThat(response.getTotalVat()).isEqualByComparingTo("57.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("357.00");
    }
}
