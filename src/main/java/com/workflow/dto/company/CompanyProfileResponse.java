package com.workflow.dto.company;

import com.workflow.entity.Company;

import java.time.LocalDateTime;

public record CompanyProfileResponse(
        Long id,
        String name,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        String town,
        String country,
        String postcode,
        String telephone,
        String mobile,
        String fax,
        String email,
        String contactEmail,
        String contactNumber,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CompanyProfileResponse fromEntity(Company company) {
        return new CompanyProfileResponse(
                company.getId(),
                company.getName(),
                company.getAddressLine1(),
                company.getAddressLine2(),
                company.getAddressLine3(),
                company.getTown(),
                company.getCountry(),
                company.getPostcode(),
                company.getTelephone(),
                company.getMobile(),
                company.getFax(),
                company.getEmail(),
                company.getContactEmail(),
                company.getContactNumber(),
                company.isArchived(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}