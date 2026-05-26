package com.workflow.templates.pdf.estimate;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EstimateTemplateData {

    // Document metadata
    String documentNumber;
    String estimateDate;
    String validUntil;     // formatted string, null if not set
    String reference;      // null if not set

    // Company
    String companyName;
    List<String> companyAddressLines;
    String vatNumber;      // null if not set
    String companyEmail;   // null if not set
    String companyPhone;   // null if not set

    // Customer
    String customerName;
    List<String> customerAddressLines;

    // Line items
    List<LineItemRow> lineItems;

    // Totals
    String subtotal;
    String vatLabel;       // "TOTAL  NO VAT" or "TOTAL VAT"
    String totalVat;
    String grandTotal;

    // Notes (null if empty)
    String notes;

    // Footer address (single line)
    String footerAddress;

    @Data
    @Builder
    public static class LineItemRow {
        String name;               // product code
        String description;
        String additionalDetails;  // null if empty
        String quantity;
        String unitPrice;
        String vatDisplay;         // e.g. "No VAT" or "19%"
        String amount;
    }
}
