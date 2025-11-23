package com.workflow.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientUpdateRequest {
    @NotBlank(message = "Client name is required")
    @Size(min = 2, max = 150, message = "Client name must be between 2 and 150 characters")
    private String name;

    private String email;
    private String telephone;
    private String mobile;
    private String address;
    private boolean archived;
}

