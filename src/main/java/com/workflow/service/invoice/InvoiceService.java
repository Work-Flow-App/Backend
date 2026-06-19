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
import com.workflow.common.constant.financial.LineItemStatus;
import com.workflow.common.constant.financial.SnapshotType;
import com.workflow.entity.financial.Estimate;
import com.workflow.entity.financial.EstimateLineItem;
import com.workflow.entity.financial.Invoice;
import com.workflow.entity.financial.JobLineItemSnapshot;
import com.workflow.repository.financial.EstimateLineItemRepository;
import com.workflow.repository.financial.EstimateRepository;
import com.workflow.repository.financial.InvoiceRepository;
import com.workflow.service.sequence.CompanyCounterService;
import com.workflow.service.storage.IStorageService;
import com.workflow.templates.pdf.invoice.InvoicePdfRenderer;
import com.workflow.templates.pdf.invoice.InvoiceTemplateData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InvoiceService implements IInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final EstimateRepository estimateRepository;
    private final EstimateLineItemRepository estimateLineItemRepository;
    private final IStorageService storageService;
    private final InvoicePdfRenderer pdfRenderer;
    private final CompanyCounterService companyCounterService;

    @Override
    public InvoiceResponse generateInvoice(Long estimateId, InvoiceCreateRequest request, Long companyId) {
        Estimate estimate = estimateRepository.findByIdWithDetailsAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        Set<Long> estimateLineItemIds = estimate.getLineItems().stream()
                .map(EstimateLineItem::getId)
                .collect(Collectors.toSet());

        List<Long> requestedIds = request.getLineItemIds();
        List<Long> invalidIds = requestedIds.stream()
                .filter(id -> !estimateLineItemIds.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new InvalidRequestException(
                    "Some selected line items do not belong to this estimate. Please verify your selection.");
        }

        List<EstimateLineItem> selectedItems = estimate.getLineItems().stream()
                .filter(eli -> requestedIds.contains(eli.getId()))
                .collect(Collectors.toList());

        List<Long> conflicts = selectedItems.stream()
                .filter(eli -> eli.getStatus() == LineItemStatus.INVOICED)
                .map(EstimateLineItem::getId)
                .toList();

        if (!conflicts.isEmpty()) {
            throw new InvalidRequestException(
                    "Some selected line items have already been invoiced: " + conflicts);
        }

        BigDecimal totalNet = selectedItems.stream()
                .map(EstimateLineItem::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVat = selectedItems.stream()
                .map(EstimateLineItem::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = selectedItems.stream()
                .map(EstimateLineItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<JobLineItemSnapshot> snapshots = selectedItems.stream()
                .map(eli -> JobLineItemSnapshot.builder()
                        .type(SnapshotType.INVOICE)
                        .sourceLineItemId(eli.getId())
                        .productCode(eli.getProductCode())
                        .productDescription(eli.getProductDescription())
                        .additionalDetails(eli.getAdditionalDetails())
                        .unitPrice(eli.getUnitPrice())
                        .quantity(eli.getQuantity())
                        .vatRate(eli.getVatRate())
                        .netAmount(eli.getNetAmount())
                        .vatAmount(eli.getVatAmount())
                        .totalAmount(eli.getTotalAmount())
                        .build())
                .collect(Collectors.toList());

        // Generate invoice number first — REQUIRES_NEW transaction guarantees uniqueness
        long invoiceSeq = companyCounterService.nextInvoiceId(companyId);
        String invoiceNumber = String.format("INV-%d-%05d", LocalDate.now().getYear(), invoiceSeq);
        String s3Key = String.format("invoices/%d/%s.pdf", companyId, invoiceNumber);

        Invoice invoiceToSave = Invoice.builder()
                .estimate(estimate)
                .company(estimate.getCompany())
                .invoiceNumber(invoiceNumber)
                .s3Key(s3Key)
                .lineItemSnapshots(snapshots)
                .dueDate(request.getDueDate())
                .reference(request.getReference())
                .totalNet(totalNet)
                .totalVat(totalVat)
                .grandTotal(grandTotal)
                .build();

        snapshots.forEach(s -> s.setInvoice(invoiceToSave));
        Invoice invoice = invoiceRepository.save(invoiceToSave);

        List<Long> selectedIds = selectedItems.stream().map(EstimateLineItem::getId).toList();
        estimateLineItemRepository.markAsInvoiced(selectedIds);

        // PDF generation and upload after TX commits — X locks on invoices and
        // job_line_item_snapshots released before CPU/S3 work.
        final Invoice committedInvoice = invoice;
        final List<EstimateLineItem> itemsForPdf = selectedItems;
        final Estimate estimateForPdf = estimate;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            log.debug("[Invoice] Registering PDF+S3 upload for afterCommit key={}", s3Key);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        log.debug("[Invoice] afterCommit: generating PDF key={}", s3Key);
                        byte[] pdfBytes = generatePdf(committedInvoice, invoiceNumber, itemsForPdf, estimateForPdf);
                        log.debug("[Invoice] afterCommit: uploading to S3 key={}", s3Key);
                        storageService.upload(s3Key, new ByteArrayInputStream(pdfBytes), pdfBytes.length, "application/pdf");
                        log.info("[Invoice] S3 upload complete key={}", s3Key);
                    } catch (Exception e) {
                        log.error("[Invoice] PDF/S3 failed key={}", s3Key, e);
                        throw e;
                    }
                }
            });
        } else {
            log.warn("[Invoice] No active TX sync — generating PDF and uploading S3 inline key={}", s3Key);
            byte[] pdfBytes = generatePdf(invoice, invoiceNumber, selectedItems, estimate);
            storageService.upload(s3Key, new ByteArrayInputStream(pdfBytes), pdfBytes.length, "application/pdf");
        }

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

    private byte[] generatePdf(Invoice invoice, String invoiceNumber, List<EstimateLineItem> items, Estimate estimate) {
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
                .customerName(customer != null ? customer.getName() : null)
                .customerAddressLines(customer != null ? customerAddressLines(customer) : List.of())
                .lineItems(items.stream().map(eli -> InvoiceTemplateData.LineItemRow.builder()
                        .description(eli.getProductDescription())
                        .additionalDetails(eli.getAdditionalDetails())
                        .quantity(eli.getQuantity().stripTrailingZeros().toPlainString())
                        .unitPrice(formatAmount(eli.getUnitPrice()))
                        .vatDisplay(eli.getVatRate().compareTo(BigDecimal.ZERO) == 0
                                ? "No VAT"
                                : eli.getVatRate().stripTrailingZeros().toPlainString() + "%")
                        .amount(formatAmount(eli.getTotalAmount()))
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
