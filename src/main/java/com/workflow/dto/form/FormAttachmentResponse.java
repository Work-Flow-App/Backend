package com.workflow.dto.form;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormAttachmentResponse {
    private Long fieldId;
    private String fieldLabel;
    private String fileName;
    private String fileUrl;
    private String fileType;
}