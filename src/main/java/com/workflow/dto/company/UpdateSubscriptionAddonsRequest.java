package com.workflow.dto.company;

import jakarta.validation.constraints.Min;

/**
 * Partial-update request for PATCH /companies/subscription/addons. Both fields are ABSOLUTE
 * ("set extra seats to N"), not incremental, and nullable = "leave this one unchanged" — deliberately
 * distinct from CreateCheckoutSessionRequest, where null collapses to 0. Collapsing null to 0 here
 * would make it impossible to change only one of the two fields.
 */
public record UpdateSubscriptionAddonsRequest(
        @Min(value = 0, message = "Extra seats cannot be negative")
        Integer extraSeats,

        @Min(value = 0, message = "Extra storage blocks cannot be negative")
        Integer extraStorageBlocks
) {}
