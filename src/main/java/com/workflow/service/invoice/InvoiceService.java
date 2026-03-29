package com.workflow.service.invoice;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.workflow.common.exception.business.EstimateNotFoundException;
import com.workflow.common.exception.business.InvoiceNotFoundException;
import com.workflow.common.exception.business.LineItemNotFoundException;
import com.workflow.dto.invoice.InvoiceCreateRequest;
import com.workflow.dto.invoice.InvoiceResponse;
import com.workflow.entity.*;
import com.workflow.repository.EstimateRepository;
import com.workflow.repository.InvoiceRepository;
import com.workflow.service.storage.IStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Company company = estimate.getCompany();
            Customer customer = estimate.getJob().getCustomer();

            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(33, 33, 33));
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(33, 33, 33));
            Font normalFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(66, 66, 66));
            Font smallBoldFont = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(33, 33, 33));
            Font tableHeaderFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
            Font totalFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(33, 33, 33));

            // ---- Top: company name left, INVOICE right ----
            PdfPTable topTable = new PdfPTable(2);
            topTable.setWidthPercentage(100);
            topTable.setSpacingAfter(20);

            PdfPCell companyNameCell = new PdfPCell(new Phrase(company.getName(), titleFont));
            companyNameCell.setBorder(Rectangle.NO_BORDER);
            companyNameCell.setVerticalAlignment(Element.ALIGN_BOTTOM);

            PdfPCell invoiceLabelCell = new PdfPCell(new Phrase("INVOICE", titleFont));
            invoiceLabelCell.setBorder(Rectangle.NO_BORDER);
            invoiceLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            invoiceLabelCell.setVerticalAlignment(Element.ALIGN_BOTTOM);

            topTable.addCell(companyNameCell);
            topTable.addCell(invoiceLabelCell);
            document.add(topTable);

            // ---- From / Bill To / Invoice Details ----
            PdfPTable infoTable = new PdfPTable(3);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{3f, 3f, 2f});
            infoTable.setSpacingAfter(20);

            // Company address
            PdfPCell fromCell = buildInfoCell("FROM", buildCompanyAddress(company), headerFont, normalFont);
            // Customer address
            PdfPCell billToCell = buildInfoCell("BILL TO", buildCustomerAddress(customer), headerFont, normalFont);
            // Invoice metadata
            StringBuilder meta = new StringBuilder();
            meta.append("Invoice No:  ").append(invoiceNumber).append("\n");
            meta.append("Date:             ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            if (invoice.getDueDate() != null) {
                meta.append("\nDue Date:      ").append(invoice.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            }
            if (invoice.getReference() != null && !invoice.getReference().isBlank()) {
                meta.append("\nReference:    ").append(invoice.getReference());
            }
            PdfPCell metaCell = buildInfoCell("DETAILS", meta.toString(), headerFont, normalFont);
            metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            infoTable.addCell(fromCell);
            infoTable.addCell(billToCell);
            infoTable.addCell(metaCell);
            document.add(infoTable);

            // ---- Line items table ----
            PdfPTable lineTable = new PdfPTable(8);
            lineTable.setWidthPercentage(100);
            lineTable.setWidths(new float[]{1.2f, 2.8f, 1f, 1.2f, 1.2f, 1f, 1.2f, 1.2f});
            lineTable.setSpacingAfter(10);

            Color headerBg = new Color(41, 128, 185);
            String[] headers = {"Code", "Description", "Qty", "Unit Price", "Net", "VAT %", "VAT Amt", "Total"};
            for (String h : headers) {
                PdfPCell hCell = new PdfPCell(new Phrase(h, tableHeaderFont));
                hCell.setBackgroundColor(headerBg);
                hCell.setPadding(5);
                hCell.setBorder(Rectangle.NO_BORDER);
                lineTable.addCell(hCell);
            }

            Color rowAlt = new Color(235, 245, 251);
            boolean alt = false;
            for (LineItem li : items) {
                Color rowBg = alt ? rowAlt : Color.WHITE;
                addLineItemRow(lineTable, li, normalFont, rowBg);
                alt = !alt;
            }
            document.add(lineTable);

            // ---- Totals ----
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(40);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setSpacingAfter(20);

            addTotalRow(totalsTable, "Total Net:", formatAmount(invoice.getTotalNet()), normalFont, smallBoldFont, false);
            addTotalRow(totalsTable, "Total VAT:", formatAmount(invoice.getTotalVat()), normalFont, smallBoldFont, false);
            addTotalRow(totalsTable, "Grand Total:", formatAmount(invoice.getGrandTotal()), totalFont, totalFont, true);
            document.add(totalsTable);

            // ---- Notes ----
            if (estimate.getNotes() != null && !estimate.getNotes().isBlank()) {
                document.add(new Paragraph("Notes", headerFont));
                Paragraph notesPara = new Paragraph(estimate.getNotes(), normalFont);
                notesPara.setSpacingBefore(4);
                document.add(notesPara);
            }

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    private PdfPCell buildInfoCell(String label, String content, Font labelFont, Font contentFont) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", labelFont));
        p.add(new Chunk(content, contentFont));
        PdfPCell cell = new PdfPCell(p);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(8);
        return cell;
    }

    private String buildCompanyAddress(Company company) {
        StringBuilder sb = new StringBuilder();
        if (company.getAddress() != null) {
            CompanyAddress addr = company.getAddress();
            appendIfPresent(sb, addr.getAddressLine1());
            appendIfPresent(sb, addr.getAddressLine2());
            appendIfPresent(sb, addr.getAddressLine3());
            appendIfPresent(sb, addr.getTown());
            appendIfPresent(sb, addr.getPostcode());
            appendIfPresent(sb, addr.getCountry());
        }
        appendIfPresent(sb, company.getEmail());
        appendIfPresent(sb, company.getTelephone());
        return sb.toString().trim();
    }

    private String buildCustomerAddress(Customer customer) {
        StringBuilder sb = new StringBuilder();
        sb.append(customer.getName()).append("\n");
        if (customer.getAddress() != null) {
            CustomerAddress addr = customer.getAddress();
            appendIfPresent(sb, addr.getHouseNumber() != null && addr.getStreet() != null
                    ? addr.getHouseNumber() + " " + addr.getStreet()
                    : addr.getStreet());
            appendIfPresent(sb, addr.getCity());
            appendIfPresent(sb, addr.getCounty());
            appendIfPresent(sb, addr.getPostalCode());
            appendIfPresent(sb, addr.getCountry());
        }
        appendIfPresent(sb, customer.getEmail());
        appendIfPresent(sb, customer.getTelephone());
        return sb.toString().trim();
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append("\n");
        }
    }

    private void addLineItemRow(PdfPTable table, LineItem li, Font font, Color bg) {
        String[] values = {
                li.getProductCode(),
                li.getProductDescription(),
                li.getQuantity().stripTrailingZeros().toPlainString(),
                formatAmount(li.getUnitPrice()),
                formatAmount(li.getNetAmount()),
                li.getVatRate().stripTrailingZeros().toPlainString() + "%",
                formatAmount(li.getVatAmount()),
                formatAmount(li.getTotalAmount())
        };
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(v, font));
            cell.setBackgroundColor(bg);
            cell.setPadding(4);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(new Color(200, 200, 200));
            table.addCell(cell);
        }
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont, boolean highlight) {
        Font effectiveLabelFont = labelFont;
        Font effectiveValueFont = valueFont;
        if (highlight) {
            effectiveLabelFont = new Font(labelFont.getFamily(), labelFont.getSize(), labelFont.getStyle(), Color.WHITE);
            effectiveValueFont = new Font(valueFont.getFamily(), valueFont.getSize(), valueFont.getStyle(), Color.WHITE);
        }

        PdfPCell labelCell = new PdfPCell(new Phrase(label, effectiveLabelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);
        if (highlight) labelCell.setBackgroundColor(new Color(41, 128, 185));

        PdfPCell valueCell = new PdfPCell(new Phrase(value, effectiveValueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(3);
        if (highlight) valueCell.setBackgroundColor(new Color(41, 128, 185));

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatAmount(BigDecimal amount) {
        return "£" + String.format("%.2f", amount);
    }
}
