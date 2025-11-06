package com.workflow.dto.client;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientCreateRequest {
    private String name;
    private String email;
    private String telephone;
    private String mobile;
    private String address;
}
