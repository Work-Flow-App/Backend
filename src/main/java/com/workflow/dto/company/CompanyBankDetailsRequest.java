package com.workflow.dto.company;

import jakarta.validation.constraints.Size;

public record CompanyBankDetailsRequest(

        @Size(max = 100, message = "Bank name cannot exceed 100 characters")
        String bankName,

        @Size(max = 100, message = "Account name cannot exceed 100 characters")
        String accountName,

        @Size(max = 50, message = "Account number cannot exceed 50 characters")
        String accountNo,

        @Size(max = 20, message = "Sort code cannot exceed 20 characters")
        String sortCode
) {}
