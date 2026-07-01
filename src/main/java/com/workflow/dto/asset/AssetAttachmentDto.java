package com.workflow.dto.asset;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetAttachmentDto {
    private String fileName;
    private String fileType;
    private String fileUrl;
}