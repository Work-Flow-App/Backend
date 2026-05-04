package com.workflow.dto.company;

import com.workflow.common.constant.Currency;
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
        Currency currency,
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
                company.getCurrency(),
                company.isArchived(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}