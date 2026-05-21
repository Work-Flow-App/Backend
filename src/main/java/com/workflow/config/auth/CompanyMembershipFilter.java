package com.workflow.config.auth;

import com.workflow.common.constant.Role;
import com.workflow.common.security.CompanyContext;
import com.workflow.common.security.CompanyContextHolder;
import com.workflow.entity.auth.User;
import com.workflow.entity.company.CompanyMember;
import com.workflow.repository.company.CompanyMemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Resolves company membership once per request for Role.COMPANY users.
 * Stores the result in CompanyContextHolder so controllers and the
 * CompanyRoleAspect can read companyId + companyRole without any further DB hits.
 *
 * NOT registered as @Component — manually added in SecurityConfig to prevent
 * double-registration via FilterRegistrationBean.
 */
@Slf4j
@RequiredArgsConstructor
public class CompanyMembershipFilter extends OncePerRequestFilter {

    private final CompanyMemberRepository companyMemberRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user
                && user.getRole() == Role.COMPANY) {

            Optional<CompanyMember> memberOpt = companyMemberRepository.findByUserId(user.getId());

            if (memberOpt.isPresent()) {
                CompanyMember member = memberOpt.get();
                CompanyContextHolder.set(new CompanyContext(
                        member.getCompany().getId(),
                        user.getId(),
                        member.getCompanyRole()
                ));
            } else {
                log.warn("COMPANY user {} has no company_members row — denying request", user.getId());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not a member of any company");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
