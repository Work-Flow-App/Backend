package com.workflow.templates.pdf.form;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class FormTemplateData {

    // Form metadata
    private String title;
    private String templateName;
    private String status;
    private String submittedAt; 
    private String workerName; 

    // Company metadata
    private String companyName;
    private List<String> companyAddressLines;
    private String companyEmail; 
    private String companyPhone; 

    // Field responses
    private List<FormFieldRow> fields;

    @Data
    @Builder
    public static class FormFieldRow {
        private String label;
        private String value; 
        private String fileName; 
        
        // --- ADDED FOR IMAGES ---
        private String fileUrl; 
        private boolean isImage; 
    }
}