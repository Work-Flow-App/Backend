package com.workflow.templates.pdf.invoice;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InvoiceTemplateData {

    // Invoice metadata
    String invoiceNumber;
    String invoiceDate;
    String dueDate;       // formatted string, null if not set
    String reference;     // null if not set

    // Company
    String companyName;
    List<String> companyAddressLines;
    String vatNumber;     // null if not set

    // Customer
    String customerName;
    List<String> customerAddressLines;

    // Line items
    List<LineItemRow> lineItems;

    // Totals
    String subtotal;
    String vatLabel;      // "TOTAL  NO VAT" or "TOTAL VAT"
    String totalVat;
    String grandTotal;

    // Bank details (null if not configured)
    BankDetailsRow bankDetails;

    // Notes (null if empty)
    String notes;

    // Footer address (single line)
    String footerAddress;

    @Data
    @Builder
    public static class LineItemRow {
        String description;
        String additionalDetails;  // null if empty
        String quantity;
        String unitPrice;
        String vatDisplay;         // e.g. "No VAT" or "19%"
        String amount;
    }

    @Data
    @Builder
    public static class BankDetailsRow {
        String bankName;
        String accountName;
        String accountNo;
        String sortCode;
    }
}
