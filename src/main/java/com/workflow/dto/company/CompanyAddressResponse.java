package com.workflow.dto.company;

import com.workflow.entity.company.CompanyAddress;

public record CompanyAddressResponse(
        String addressLine1,
        String addressLine2,
        String addressLine3,
        String town,
        String country,
        String postcode
) {
    public static CompanyAddressResponse fromEntity(CompanyAddress address) {
        if (address == null) return null;
        return new CompanyAddressResponse(
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getAddressLine3(),
                address.getTown(),
                address.getCountry(),
                address.getPostcode()
        );
    }
}
