package com.workflow.dto.company;

import com.workflow.entity.company.Company;

import java.time.LocalDateTime;

public record CompanyProfileResponse(
        Long id,
        String name,
        CompanyAddressResponse address,
        String telephone,
        String mobile,
        String fax,
        String email,
        String contactEmail,
        String contactNumber,
        String vatNumber,
        CompanyBankDetailsResponse bankDetails,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CompanyProfileResponse fromEntity(Company company) {
        return new CompanyProfileResponse(
                company.getId(),
                company.getName(),
                CompanyAddressResponse.fromEntity(company.getAddress()),
                company.getTelephone(),
                company.getMobile(),
                company.getFax(),
                company.getEmail(),
                company.getContactEmail(),
                company.getContactNumber(),
                company.getVatNumber(),
                CompanyBankDetailsResponse.fromEntity(company.getBankDetails()),
                company.isArchived(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}