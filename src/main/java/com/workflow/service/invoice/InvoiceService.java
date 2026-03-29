package com.workflow.service.invoice;

import com.workflow.common.exception.business.EstimateNotFoundException;
import com.workflow.common.exception.business.InvoiceNotFoundException;
import com.workflow.common.exception.business.LineItemNotFoundException;
import com.workflow.dto.invoice.InvoiceCreateRequest;
import com.workflow.dto.invoice.InvoiceResponse;
import com.workflow.entity.*;
import com.workflow.repository.EstimateRepository;
import com.workflow.repository.InvoiceRepository;
import com.workflow.service.storage.IStorageService;
import com.workflow.templates.pdf.invoice.InvoicePdfRenderer;
import com.workflow.templates.pdf.invoice.InvoiceTemplateData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class InvoiceService implements IInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final EstimateRepository estimateRepository;
    private final IStorageService storageService;
    private final InvoicePdfRenderer pdfRenderer;

    @Override
    public InvoiceResponse generateInvoice(Long estimateId, InvoiceCreateRequest request, Long companyId) {
        Estimate estimate = estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        Set<Long> estimateLineItemIds = estimate.getLineItems().stream()
                .map(LineItem::getId)
                .collect(Collectors.toSet());

        List<Long> requestedIds = request.getLineItemIds();
        List<Long> invalidIds = requestedIds.stream()
                .filter(id -> !estimateLineItemIds.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new LineItemNotFoundException(
                    "Line items not found in this estimate: " + invalidIds);
        }

        List<LineItem> selectedItems = estimate.getLineItems().stream()
                .filter(li -> requestedIds.contains(li.getId()))
                .collect(Collectors.toList());

        BigDecimal totalNet = selectedItems.stream()
                .map(LineItem::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVat = selectedItems.stream()
                .map(LineItem::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = selectedItems.stream()
                .map(LineItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Save invoice to get the generated ID for the invoice number
        Invoice invoice = Invoice.builder()
                .estimate(estimate)
                .company(estimate.getCompany())
                .invoiceNumber("PENDING")
                .s3Key("PENDING")
                .lineItems(selectedItems)
                .dueDate(request.getDueDate())
                .reference(request.getReference())
                .totalNet(totalNet)
                .totalVat(totalVat)
                .grandTotal(grandTotal)
                .build();

        invoice = invoiceRepository.save(invoice);

        String invoiceNumber = String.format("INV-%d-%05d", LocalDate.now().getYear(), invoice.getId());
        String s3Key = String.format("invoices/%d/%s.pdf", companyId, invoiceNumber);

        byte[] pdfBytes = generatePdf(invoice, invoiceNumber, selectedItems, estimate);
        storageService.upload(s3Key, new ByteArrayInputStream(pdfBytes), pdfBytes.length, "application/pdf");

        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setS3Key(s3Key);
        invoice = invoiceRepository.save(invoice);

        String presignedUrl = storageService.generatePresignedUrl(s3Key);
        return InvoiceResponse.fromEntity(invoice, presignedUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesForEstimate(Long estimateId, Long companyId) {
        estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        return invoiceRepository.findByEstimateIdAndCompanyId(estimateId, companyId).stream()
                .map(inv -> InvoiceResponse.fromEntity(inv, storageService.generatePresignedUrl(inv.getS3Key())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long invoiceId, Long companyId) {
        Invoice invoice = invoiceRepository.findByIdAndCompanyId(invoiceId, companyId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found"));
        String presignedUrl = storageService.generatePresignedUrl(invoice.getS3Key());
        return InvoiceResponse.fromEntity(invoice, presignedUrl);
    }

    private byte[] generatePdf(Invoice invoice, String invoiceNumber, List<LineItem> items, Estimate estimate) {
        Company company = estimate.getCompany();
        Customer customer = estimate.getJob().getCustomer();
        CompanyBankDetails bankDetails = company.getBankDetails();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");

        InvoiceTemplateData data = InvoiceTemplateData.builder()
                .invoiceNumber(invoiceNumber)
                .invoiceDate(LocalDate.now().format(dateFmt))
                .dueDate(invoice.getDueDate() != null ? invoice.getDueDate().format(dateFmt) : null)
                .reference(invoice.getReference())
                .companyName(company.getName())
                .companyAddressLines(companyAddressLines(company))
                .vatNumber(company.getVatNumber())
                .customerName(customer.getName())
                .customerAddressLines(customerAddressLines(customer))
                .lineItems(items.stream().map(li -> InvoiceTemplateData.LineItemRow.builder()
                        .description(li.getProductDescription())
                        .additionalDetails(li.getAdditionalDetails())
                        .quantity(li.getQuantity().stripTrailingZeros().toPlainString())
                        .unitPrice(formatAmount(li.getUnitPrice()))
                        .vatDisplay(li.getVatRate().compareTo(BigDecimal.ZERO) == 0
                                ? "No VAT"
                                : li.getVatRate().stripTrailingZeros().toPlainString() + "%")
                        .amount(formatAmount(li.getTotalAmount()))
                        .build()).collect(Collectors.toList()))
                .subtotal(formatAmount(invoice.getTotalNet()))
                .vatLabel(invoice.getTotalVat().compareTo(BigDecimal.ZERO) == 0 ? "TOTAL  NO VAT" : "TOTAL VAT")
                .totalVat(formatAmount(invoice.getTotalVat()))
                .grandTotal(formatAmount(invoice.getGrandTotal()))
                .bankDetails(bankDetails != null ? InvoiceTemplateData.BankDetailsRow.builder()
                        .bankName(nullSafe(bankDetails.getBankName()))
                        .accountName(nullSafe(bankDetails.getAccountName()))
                        .accountNo(nullSafe(bankDetails.getAccountNo()))
                        .sortCode(nullSafe(bankDetails.getSortCode()))
                        .build() : null)
                .notes(estimate.getNotes())
                .footerAddress(String.join(", ", companyAddressLines(company)))
                .build();

        return pdfRenderer.render(data);
    }

    private List<String> companyAddressLines(Company company) {
        List<String> lines = new ArrayList<>();
        if (company.getAddress() != null) {
            CompanyAddress addr = company.getAddress();
            addIfPresent(lines, addr.getAddressLine1());
            addIfPresent(lines, addr.getAddressLine2());
            addIfPresent(lines, addr.getAddressLine3());
            addIfPresent(lines, addr.getTown());
            addIfPresent(lines, addr.getPostcode());
            addIfPresent(lines, addr.getCountry());
        }
        return lines;
    }

    private List<String> customerAddressLines(Customer customer) {
        List<String> lines = new ArrayList<>();
        if (customer.getAddress() != null) {
            CustomerAddress addr = customer.getAddress();
            String street = addr.getHouseNumber() != null && addr.getStreet() != null
                    ? addr.getHouseNumber() + " " + addr.getStreet()
                    : addr.getStreet();
            addIfPresent(lines, street);
            addIfPresent(lines, addr.getCity());
            addIfPresent(lines, addr.getCounty());
            addIfPresent(lines, addr.getPostalCode());
            addIfPresent(lines, addr.getCountry());
        }
        return lines;
    }

    private void addIfPresent(List<String> list, String value) {
        if (value != null && !value.isBlank()) list.add(value);
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}
