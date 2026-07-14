package com.workflow.dto.company;

public record PublicCompanyProfileResponse(
        Long id,
        String name,
        String logoUrl,
        String description,
        String website,
        String tagline,
        CompanyAddressResponse address
) {}