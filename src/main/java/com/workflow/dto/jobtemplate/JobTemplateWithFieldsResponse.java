package com.workflow.dto.jobtemplate;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTemplateWithFieldsResponse {
    private JobTemplateResponse template;
    private List<JobTemplateFieldResponse> fields;
}
