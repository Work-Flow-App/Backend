package com.workflow.dto.jobtemplate;

import com.workflow.common.constant.job.JobFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTemplateFieldCreateRequest {

    @NotNull(message = "Template ID is required")
    private Long templateId;

    @NotBlank(message = "Field name is required")
    @Size(max = 150, message = "Field name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Field label is required")
    @Size(max = 150, message = "Field label cannot exceed 150 characters")
    private String label;

    @NotNull(message = "Field type is required")
    private JobFieldType jobFieldType;

    private boolean required;

    private String options;  // JSON string for dropdown

    private Integer orderIndex;
}
