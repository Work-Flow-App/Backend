package com.workflow.templates.pdf.form;

import com.workflow.entity.company.Company;
import com.workflow.entity.form.FormSubmission;
import com.workflow.entity.form.FormFieldValue;
import com.workflow.service.storage.IStorageService; // <-- Added Import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormPdfRenderer {

    private static final String TEMPLATE = "pdf/form/form-template";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final SpringTemplateEngine templateEngine;
    private final IStorageService s3Service; // <-- Inject Storage Service

    public byte[] generatePdf(FormSubmission submission) throws Exception {
        FormTemplateData data = mapToTemplateData(submission);

        Context context = new Context();
        context.setVariable("data", data);

        String htmlContent = templateEngine.process(TEMPLATE, context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
    }

    private FormTemplateData mapToTemplateData(FormSubmission submission) {
        Company company = submission.getCompany();

        // 1. Map Address Lines
        List<String> addressLines = new ArrayList<>();
        if (company.getAddress() != null) {
            if (company.getAddress().getAddressLine1() != null)
                addressLines.add(company.getAddress().getAddressLine1());
            if (company.getAddress().getAddressLine2() != null)
                addressLines.add(company.getAddress().getAddressLine2());
            if (company.getAddress().getTown() != null)
                addressLines.add(company.getAddress().getTown());
            if (company.getAddress().getPostcode() != null)
                addressLines.add(company.getAddress().getPostcode());
        }

        // 2. Map Fields
        List<FormTemplateData.FormFieldRow> fields = submission.getFieldValues().stream()
                .map(this::mapField)
                .collect(Collectors.toList());

        // 3. Format Date
        String submittedAtStr = submission.getSubmittedAt() != null
                ? submission.getSubmittedAt().format(DATE_FORMATTER)
                : null;

        return FormTemplateData.builder()
                .title(submission.getTitle())
                .templateName(submission.getTemplate().getName())
                .status(submission.getStatus().name())
                .submittedAt(submittedAtStr)
                .workerName(submission.getWorker() != null ? submission.getWorker().getName() : "Unassigned")
                .companyName(company.getName())
                .companyAddressLines(addressLines)
                .companyEmail(company.getContactEmail() != null ? company.getContactEmail() : company.getEmail())
                .companyPhone(company.getContactNumber() != null ? company.getContactNumber() : company.getTelephone())
                .fields(fields)
                .build();
    }

    private FormTemplateData.FormFieldRow mapField(FormFieldValue val) {
        String displayValue = null;

        if (val.getStringValue() != null) {
            displayValue = val.getStringValue();
        } else if (val.getBooleanValue() != null) {
            displayValue = val.getBooleanValue() ? "Yes" : "No";
        } else if (val.getDateValue() != null) {
            displayValue = val.getDateValue().format(DATE_FORMATTER);
        } else if (val.getJsonValue() != null) {
            // Clean up JSON Arrays for MULTI_SELECT ---
            String rawJson = val.getJsonValue();
            if (rawJson.startsWith("[") && rawJson.endsWith("]")) {
                // Converts ["A","B"] into A, B
                displayValue = rawJson.replaceAll("[\\[\\]\"]", "").replace(",", ", ");
            } else {
                displayValue = rawJson;
            }
        }

        // --- NEW LOGIC TO HANDLE IMAGES ---
        boolean isImage = false;
        String resolvedUrl = null;

        if (val.getFileUrl() != null) {
            resolvedUrl = s3Service.resolveFileUrl(val.getFileUrl());
            // Check if the uploaded file was actually an image (e.g., image/png,
            // image/jpeg)
            if (val.getFileType() != null && val.getFileType().startsWith("image/")) {
                isImage = true;
            }
        }

        return FormTemplateData.FormFieldRow.builder()
                .label(val.getField().getLabel())
                .value(displayValue)
                .fileName(val.getFileName())
                .fileUrl(resolvedUrl) // Passing URL to HTML
                .isImage(isImage) // Flag to trigger <img> tag
                .build();
    }
}