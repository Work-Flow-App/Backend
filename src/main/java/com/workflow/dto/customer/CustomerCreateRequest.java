package com.workflow.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCreateRequest {
    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 150, message = "Customer name must be between 2 and 150 characters")
    private String name;

    private String email;
    private String telephone;
    private String mobile;
    private CustomerAddressDto address;
}