package com.workflow.dto.customer;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddressDto {
    private String houseNumber;
    private String street;
    private String city;
    private String county;
    private String postalCode;
    private String country;
}
