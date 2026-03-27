package com.workflow.dto.customer;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {
    private Long id;
    private Long customerRef;
    private String name;
    private String email;
    private String telephone;
    private String mobile;
    private CustomerAddressDto address;
    private boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}