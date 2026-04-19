package com.workflow.service.invoice;

import com.workflow.common.exception.business.EstimateNotFoundException;
import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.common.exception.business.InvoiceNotFoundException;
import com.workflow.dto.invoice.InvoiceCreateRequest;
import com.workflow.dto.invoice.InvoiceResponse;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanyAddress;
import com.workflow.entity.company.CompanyBankDetails;
import com.workflow.entity.customer.Customer;
import com.workflow.entity.customer.CustomerAddress;
import com.workflow.entity.financial.Estimate;
import com.workflow.entity.financial.Invoice;
import com.workflow.entity.financial.LineItem;
import com.workflow.repository.financial.EstimateRepository;
import com.workflow.repository.financial.InvoiceRepository;
import com.workflow.repository.financial.LineItemRepository;
import com.workflow.service.sequence.CompanyCounterService;
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
    private final LineItemRepository lineItemRepository;
    private final IStorageService storageService;
    private final InvoicePdfRenderer pdfRenderer;
    private final CompanyCounterService companyCounterService;

    @Override
    public InvoiceResponse generateInvoice(Long estimateId, InvoiceCreateRequest request, Long companyId) {
        Estimate estimate = estimateRepository.findByIdWithDetailsAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        Set<Long> estimateLineItemIds = estimate.getLineItems().stream()
                .map(LineItem::getId)
                .collect(Collectors.toSet());

        List<Long> requestedIds = request.getLineItemIds();
        List<Long> invalidIds = requestedIds.stream()
                .filter(id -> !estimateLineItemIds.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new InvalidRequestException(
                    "Some selected line items do not belong to this estimate. Please verify your selection.");
        }

        List<LineItem> selectedItems = estimate.getLineItems().stream()
                .filter(li -> requestedIds.contains(li.getId()))
                .collect(Collectors.toList());

        List<Long> alreadyInvoicedIds = selectedItems.stream()
                .filter(LineItem::isInvoiced)
                .map(LineItem::getId)
                .toList();

        if (!alreadyInvoicedIds.isEmpty()) {
            throw new InvalidRequestException(
                    "Some selected line items have already been invoiced: " + alreadyInvoicedIds +
                    ". Please remove them from your selection.");
        }

        BigDecimal totalNet = selectedItems.stream()
                .map(LineItem::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVat = selectedItems.stream()
                .map(LineItem::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = selectedItems.stream()
                .map(LineItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Generate invoice number first — REQUIRES_NEW transaction guarantees uniqueness
        long invoiceSeq = companyCounterService.nextInvoiceId(companyId);
        String invoiceNumber = String.format("INV-%d-%05d", LocalDate.now().getYear(), invoiceSeq);
        String s3Key = String.format("invoices/%d/%s.pdf", companyId, invoiceNumber);

        Invoice invoice = Invoice.builder()
                .estimate(estimate)
                .company(estimate.getCompany())
                .invoiceNumber(invoiceNumber)
                .s3Key(s3Key)
                .lineItems(selectedItems)
                .dueDate(request.getDueDate())
                .reference(request.getReference())
                .totalNet(totalNet)
                .totalVat(totalVat)
                .grandTotal(grandTotal)
                .build();

        invoice = invoiceRepository.save(invoice);

        byte[] pdfBytes = generatePdf(invoice, invoiceNumber, selectedItems, estimate);
        storageService.upload(s3Key, new ByteArrayInputStream(pdfBytes), pdfBytes.length, "application/pdf");

        List<Long> selectedIds = selectedItems.stream().map(LineItem::getId).toList();
        lineItemRepository.markAsInvoiced(selectedIds);

        String presignedUrl = storageService.generatePresignedUrl(s3Key);
        return InvoiceResponse.fromEntity(invoice, presignedUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices(Long companyId) {
        return invoiceRepository.findAllByCompanyId(companyId).stream()
                .map(inv -> InvoiceResponse.fromEntity(inv, storageService.generatePresignedUrl(inv.getS3Key())))
                .collect(Collectors.toList());
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
