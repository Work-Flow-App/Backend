package com.workflow.dto.jobtemplate;

import com.workflow.common.constant.job.JobFieldType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTemplateFieldCreateRequest {
    private Long templateId;
    private String name;
    private String label;
    private JobFieldType jobFieldType;
    private boolean required;
    private String options;  // JSON string for dropdown
    private Integer orderIndex;
}
