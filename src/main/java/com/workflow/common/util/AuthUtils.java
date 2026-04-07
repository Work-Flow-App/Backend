package com.workflow.common.util;

import com.workflow.entity.User;
import com.workflow.service.company.ICompanyService;
import org.springframework.security.core.Authentication;

/**
 * Shared utility for extracting the authenticated company ID from a Spring
 * Security {@link Authentication} principal.
 *
 * <p>Eliminates the duplicated {@code getCompanyId(Authentication)} helper that
 * was repeated in 10+ controllers.
 */
public final class AuthUtils {

    private AuthUtils() {
        // utility class — no instantiation
    }

    /**
     * Resolves the company ID for the currently authenticated user.
     *
     * @param authentication the Spring Security authentication object
     * @param companyService service used to look up the company by user ID
     * @return the company ID associated with the authenticated user
     */
    public static Long getCompanyId(Authentication authentication, ICompanyService companyService) {
        User user = (User) authentication.getPrincipal();
        return companyService.findCompanyByUserId(user.getId()).getId();
    }
}
