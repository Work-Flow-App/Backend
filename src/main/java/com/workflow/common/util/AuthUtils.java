package com.workflow.common.util;

import com.workflow.common.security.CompanyContextHolder;
import com.workflow.entity.auth.User;
import org.springframework.security.core.Authentication;

public final class AuthUtils {

    private AuthUtils() {}

    /**
     * Returns the company ID from the current request context.
     * Populated once per request by CompanyMembershipFilter — zero DB hit.
     */
    public static Long getCompanyId() {
        return CompanyContextHolder.getCompanyId();
    }

    /**
     * Returns the authenticated User principal.
     */
    public static User getUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
