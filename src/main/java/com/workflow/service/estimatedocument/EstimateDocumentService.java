package com.workflow.service.estimatedocument;

import com.workflow.common.exception.business.EstimateDocumentNotFoundException;
import com.workflow.common.exception.business.EstimateNotFoundException;
import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.dto.estimatedocument.EstimateDocumentCreateRequest;
import com.workflow.dto.estimatedocument.EstimateDocumentResponse;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanyAddress;
import com.workflow.entity.customer.Customer;
import com.workflow.entity.customer.CustomerAddress;
import com.workflow.common.constant.financial.LineItemStatus;
import com.workflow.common.constant.financial.SnapshotType;
import com.workflow.entity.financial.Estimate;
import com.workflow.entity.financial.EstimateDocument;
import com.workflow.entity.financial.EstimateLineItem;
import com.workflow.entity.financial.JobLineItemSnapshot;
import com.workflow.repository.financial.EstimateDocumentRepository;
import com.workflow.repository.financial.EstimateLineItemRepository;
import com.workflow.repository.financial.EstimateRepository;
import com.workflow.service.sequence.CompanyCounterService;
import com.workflow.service.storage.IStorageService;
import com.workflow.templates.pdf.estimate.EstimatePdfRenderer;
import com.workflow.templates.pdf.estimate.EstimateTemplateData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
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
public class EstimateDocumentService implements IEstimateDocumentService {

    private final EstimateDocumentRepository estimateDocumentRepository;
    private final EstimateRepository estimateRepository;
    private final EstimateLineItemRepository estimateLineItemRepository;
    private final IStorageService storageService;
    private final EstimatePdfRenderer pdfRenderer;
    private final CompanyCounterService companyCounterService;

    @Override
    public EstimateDocumentResponse generateEstimateDocument(Long estimateId,
                                                              EstimateDocumentCreateRequest request,
                                                              Long companyId) {
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
                        .type(SnapshotType.ESTIMATE_DOCUMENT)
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

        // REQUIRES_NEW transaction guarantees uniqueness of the sequence counter
        long docSeq = companyCounterService.nextEstimateDocumentId(companyId);
        String documentNumber = String.format("EST-%d-%05d", LocalDate.now().getYear(), docSeq);
        String s3Key = String.format("estimate-documents/%d/%s.pdf", companyId, documentNumber);

        EstimateDocument docToSave = EstimateDocument.builder()
                .estimate(estimate)
                .company(estimate.getCompany())
                .documentNumber(documentNumber)
                .s3Key(s3Key)
                .lineItemSnapshots(snapshots)
                .totalNet(totalNet)
                .totalVat(totalVat)
                .grandTotal(grandTotal)
                .validUntil(request.getValidUntil())
                .reference(request.getReference())
                .notes(request.getNotes() != null ? request.getNotes() : estimate.getNotes())
                .build();

        // Generate PDF before any DB writes — rendering failure must not commit the document record.
        byte[] pdfBytes = generatePdf(docToSave, documentNumber, selectedItems, estimate);

        snapshots.forEach(s -> s.setEstimateDocument(docToSave));
        EstimateDocument savedDoc = estimateDocumentRepository.save(docToSave);

        // Transition AVAILABLE line items to WAITING_APPROVAL after successful PDF generation.
        List<Long> selectedIds = selectedItems.stream().map(EstimateLineItem::getId).toList();
        estimateLineItemRepository.updateStatusForIdsIfAvailable(selectedIds, LineItemStatus.WAITING_APPROVAL);

        // Upload after TX commits — releasing X locks on estimate_documents and
        // job_line_item_snapshots before the S3 network call.
        final byte[] uploadBytes = pdfBytes;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    storageService.upload(s3Key, new ByteArrayInputStream(uploadBytes), uploadBytes.length, "application/pdf");
                }
            });
        } else {
            storageService.upload(s3Key, new ByteArrayInputStream(uploadBytes), uploadBytes.length, "application/pdf");
        }

        String presignedUrl = storageService.generatePresignedUrl(s3Key);
        return EstimateDocumentResponse.fromEntity(savedDoc, presignedUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstimateDocumentResponse> getEstimateDocuments(Long estimateId, Long companyId) {
        estimateRepository.findByIdAndCompanyId(estimateId, companyId)
                .orElseThrow(() -> new EstimateNotFoundException("Estimate not found"));

        return estimateDocumentRepository.findByEstimateIdAndCompanyId(estimateId, companyId).stream()
                .map(doc -> EstimateDocumentResponse.fromEntity(
                        doc, storageService.generatePresignedUrl(doc.getS3Key())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EstimateDocumentResponse getEstimateDocument(Long documentId, Long companyId) {
        EstimateDocument doc = estimateDocumentRepository.findByIdAndCompanyId(documentId, companyId)
                .orElseThrow(() -> new EstimateDocumentNotFoundException("Estimate document not found"));
        String presignedUrl = storageService.generatePresignedUrl(doc.getS3Key());
        return EstimateDocumentResponse.fromEntity(doc, presignedUrl);
    }

    // READ_COMMITTED: CASCADE DELETE does not acquire gap locks in this isolation level,
    // eliminating contention on idx_jlis_estimate_document with concurrent generateEstimateDocument INSERTs.
    // REQUIRES_NEW: own TX so gap locks release as soon as cleanup commits, independent of any
    // outer TX or afterCommit context that called this method.
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public void cleanupEmptyDocuments(Long estimateId, Long companyId) {
        List<EstimateDocument> docs = estimateDocumentRepository
                .findByEstimateIdAndCompanyId(estimateId, companyId);

        for (EstimateDocument doc : docs) {
            List<Long> snapshotLineItemIds = doc.getLineItemSnapshots().stream()
                    .map(JobLineItemSnapshot::getSourceLineItemId)
                    .collect(Collectors.toList());

            if (snapshotLineItemIds.isEmpty()) {
                deleteDoc(doc);
                continue;
            }

            // Guard: if any snapshot source ID doesn't exist in estimate_line_items,
            // those are orphaned references from pre-V23 data. Keep the document.
            List<Long> existingIds = estimateLineItemRepository.findExistingIds(snapshotLineItemIds, estimateId);
            if (existingIds.size() < snapshotLineItemIds.size()) {
                continue;
            }

            boolean allAvailable = estimateLineItemRepository
                    .findIdsWithNonAvailableStatus(existingIds, estimateId).isEmpty();

            if (allAvailable) {
                deleteDoc(doc);
            }
        }
    }

    private void deleteDoc(EstimateDocument doc) {
        String s3Key = doc.getS3Key();
        estimateDocumentRepository.delete(doc);
        // S3 delete in background — must not block the TX so gap locks are released immediately.
        CompletableFuture.runAsync(() -> {
            try {
                storageService.delete(s3Key);
            } catch (RuntimeException ignored) {
                // best-effort; DB row already removed
            }
        });
    }

    private byte[] generatePdf(EstimateDocument doc, String documentNumber,
                                List<EstimateLineItem> items, Estimate estimate) {
        Company company = estimate.getCompany();
        Customer customer = estimate.getJob().getCustomer();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");

        EstimateTemplateData data = EstimateTemplateData.builder()
                .documentNumber(documentNumber)
                .estimateDate(LocalDate.now().format(dateFmt))
                .validUntil(doc.getValidUntil() != null ? doc.getValidUntil().format(dateFmt) : null)
                .reference(doc.getReference())
                .companyName(company.getName())
                .companyAddressLines(companyAddressLines(company))
                .vatNumber(company.getVatNumber())
                .companyEmail(company.getEmail() != null ? company.getEmail() : company.getContactEmail())
                .companyPhone(company.getTelephone() != null ? company.getTelephone() : company.getMobile())
                .customerName(customer != null ? customer.getName() : null)
                .customerAddressLines(customer != null ? customerAddressLines(customer) : List.of())
                .lineItems(items.stream().map(eli -> EstimateTemplateData.LineItemRow.builder()
                        .name(eli.getProductCode())
                        .description(eli.getProductDescription())
                        .additionalDetails(eli.getAdditionalDetails())
                        .quantity(eli.getQuantity().stripTrailingZeros().toPlainString())
                        .unitPrice(formatAmount(eli.getUnitPrice()))
                        .vatDisplay(eli.getVatRate().compareTo(BigDecimal.ZERO) == 0
                                ? "No VAT"
                                : eli.getVatRate().stripTrailingZeros().toPlainString() + "%")
                        .amount(formatAmount(eli.getTotalAmount()))
                        .build()).collect(Collectors.toList()))
                .subtotal(formatAmount(doc.getTotalNet()))
                .vatLabel(doc.getTotalVat().compareTo(BigDecimal.ZERO) == 0 ? "TOTAL  NO VAT" : "TOTAL VAT")
                .totalVat(formatAmount(doc.getTotalVat()))
                .grandTotal(formatAmount(doc.getGrandTotal()))
                .notes(doc.getNotes())
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

    private String formatAmount(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}
