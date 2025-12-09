package com.workflow.dto.job;

import com.workflow.common.constant.job.JobFieldType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldValueResponse {
    private String name;
    private String label;
    private JobFieldType type;
    private Object value;
}