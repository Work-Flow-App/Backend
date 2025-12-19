package com.workflow.dto.jobtemplate;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTemplateResponse {
    private Long id;
    private Long companyId;
    private String name;
    private String description;
    private boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
