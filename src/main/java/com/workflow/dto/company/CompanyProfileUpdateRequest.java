package com.workflow.dto.company;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyProfileUpdateRequest(
        @NotBlank(message = "Company name is required")
        @Size(min = 2, max = 150, message = "Company name must be between 2 and 150 characters")
        String name,

        @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
        String addressLine2,

        @Size(max = 255, message = "Address line 3 cannot exceed 255 characters")
        String addressLine3,

        @Size(max = 100, message = "Town cannot exceed 100 characters")
        String town,

        @Size(max = 100, message = "Country cannot exceed 100 characters")
        String country,

        @Size(max = 20, message = "Postcode cannot exceed 20 characters")
        String postcode,

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
        String contactNumber
) {}