package com.workflow.dto.client;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientUpdateRequest {
    private String name;
    private String email;
    private String telephone;
    private String mobile;
    private String address;
    private boolean archived;
}

