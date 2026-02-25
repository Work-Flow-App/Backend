package com.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class CustomerAddress {

    @Column(name = "house_number", length = 20)
    private String houseNumber;

    @Column(name = "street", length = 150)
    private String street;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "county", length = 100)
    private String county;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 100)
    private String country;
}
