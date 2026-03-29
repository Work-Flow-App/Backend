package com.workflow.dto.company;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyProfileUpdateRequest(
        @NotBlank(message = "Company name is required")
        @Size(min = 2, max = 150, message = "Company name must be between 2 and 150 characters")
        String name,

        @Valid
        CompanyAddressRequest address,

        @Size(max = 20, message = "Telephone cannot exceed 20 characters")
        String telephone,

        @Size(max = 20, message = "Mobile cannot exceed 20 characters")
        String mobile,

        @Size(max = 20, message = "Fax cannot exceed 20 characters")
        String fax,

        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        String email,

        @Email(message = "Invalid contact email format")
        @Size(max = 100, message = "Contact email cannot exceed 100 characters")
        String contactEmail,

        @Size(max = 50, message = "Contact number cannot exceed 50 characters")
        String contactNumber,

        @Valid
        CompanyBankDetailsRequest bankDetails
) {}