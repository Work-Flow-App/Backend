package com.workflow.dto.job;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String additionalInfo;
    private Double latitude;
    private Double longitude;
}
