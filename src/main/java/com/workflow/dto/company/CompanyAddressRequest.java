package com.workflow.dto.company;

import jakarta.validation.constraints.Size;

public record CompanyAddressRequest(

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
        String postcode
) {}
