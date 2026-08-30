package com.workflow.templates.pdf.form;

import com.workflow.entity.company.Company;
import com.workflow.entity.form.FormField;
import com.workflow.entity.form.FormSubmission;
import com.workflow.entity.form.FormFieldValue;
import com.workflow.service.storage.IStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormPdfRenderer {

    private static final String TEMPLATE = "pdf/form/form-template";

    // --- UPDATED: Two different formatters ---
    // 1. For user's answers (keeps the time because it's their local time)
    private static final DateTimeFormatter FIELD_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    // 2. For the submission header (omits the time to hide the UTC offset)
    private static final DateTimeFormatter SUBMISSION_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final SpringTemplateEngine templateEngine;
    private final IStorageService s3Service;

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
        List<FormTemplateData.FormFieldRow> fields = submission.getTemplate().getFields().stream()
                .sorted(Comparator.comparing(FormField::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(templateField -> {
                    FormFieldValue val = submission.getFieldValues().stream()
                            .filter(v -> v.getField().getId().equals(templateField.getId()))
                            .findFirst()
                            .orElse(null);

                    return mapField(templateField, val);
                })
                .collect(Collectors.toList());

        // 3. Format Date - USES THE NEW OMIT-TIME FORMATTER
        String submittedAtStr = submission.getSubmittedAt() != null
                ? submission.getSubmittedAt().format(SUBMISSION_DATE_FORMATTER)
                : null;

        return FormTemplateData.builder()
                .title(submission.getTitle())
                .templateName(submission.getTemplate().getName())
                .status(submission.getStatus().name())
                .submittedAt(submittedAtStr)
                .workerName(submission.getWorker() != null ? submission.getWorker().getName() : "Unassigned")
                .jobRef(submission.getJob() != null ? submission.getJob().getJobRef() : null)
                .companyName(company.getName())
                .companyAddressLines(addressLines)
                .companyEmail(company.getContactEmail() != null ? company.getContactEmail() : company.getEmail())
                .companyPhone(company.getContactNumber() != null ? company.getContactNumber() : company.getTelephone())
                .fields(fields)
                .build();
    }

    private FormTemplateData.FormFieldRow mapField(FormField field, FormFieldValue val) {
        String displayValue = null;
        boolean isImage = false;
        String resolvedUrl = null;
        String fileName = null;

        if (val != null) {
            fileName = val.getFileName();

            if (val.getStringValue() != null) {
                displayValue = val.getStringValue();
            } else if (val.getBooleanValue() != null) {
                displayValue = val.getBooleanValue() ? "Yes" : "No";
            } else if (val.getDateValue() != null) {
                // USES THE FULL FORMATTER FOR FIELD ANSWERS
                displayValue = val.getDateValue().format(FIELD_DATE_FORMATTER);
            } else if (val.getJsonValue() != null) {
                String rawJson = val.getJsonValue();
                if (rawJson.startsWith("[") && rawJson.endsWith("]")) {
                    displayValue = rawJson.replaceAll("[\\[\\]\"]", "").replace(",", ", ");
                } else {
                    displayValue = rawJson;
                }
            }

            if (val.getFileUrl() != null) {
                resolvedUrl = s3Service.resolveFileUrl(val.getFileUrl());
                if (val.getFileType() != null && val.getFileType().startsWith("image/")) {
                    isImage = true;
                }
            }
        }

        return FormTemplateData.FormFieldRow.builder()
                .label(field.getLabel())
                .value(displayValue)
                .fileName(fileName)
                .fileUrl(resolvedUrl)
                .isImage(isImage)
                .build();
    }
}