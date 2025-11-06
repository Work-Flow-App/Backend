package com.workflow.dto.client;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponse {
    private Long id;
    private String name;
    private String email;
    private String telephone;
    private String mobile;
    private String address;
    private boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}