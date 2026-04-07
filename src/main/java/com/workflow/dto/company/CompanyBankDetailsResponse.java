package com.workflow.dto.company;

import com.workflow.entity.company.CompanyBankDetails;

public record CompanyBankDetailsResponse(
        String bankName,
        String accountName,
        String accountNo,
        String sortCode
) {
    public static CompanyBankDetailsResponse fromEntity(CompanyBankDetails bankDetails) {
        if (bankDetails == null) return null;
        return new CompanyBankDetailsResponse(
                bankDetails.getBankName(),
                bankDetails.getAccountName(),
                bankDetails.getAccountNo(),
                bankDetails.getSortCode()
        );
    }
}
