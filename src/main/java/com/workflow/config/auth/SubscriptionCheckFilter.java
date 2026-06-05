package com.workflow.config.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.entity.auth.User;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.service.company.ICompanyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Checks subscription status for authenticated COMPANY users on every request.
 * NOT registered as a @Component — manually registered in SecurityConfig to prevent
 * Spring Boot's FilterRegistrationBean from also adding it to the servlet filter chain
 * (which would cause it to run twice: once in Spring Security, once outside it).
 */
@Slf4j
public class SubscriptionCheckFilter extends OncePerRequestFilter {

    private final ICompanyService companyService;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final PaddleConfigProperties paddleProps;
    private final ObjectMapper objectMapper;

    public SubscriptionCheckFilter(
            ICompanyService companyService,
            CompanySubscriptionRepository subscriptionRepository,
            PaddleConfigProperties paddleProps,
            ObjectMapper objectMapper) {
        this.companyService = companyService;
        this.subscriptionRepository = subscriptionRepository;
        this.paddleProps = paddleProps;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Skip unauthenticated requests — JwtFilter handles those
        if (auth == null || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only enforce for COMPANY role
        boolean isCompany = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY"));
        if (!isCompany) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();

        // Skip subscription management endpoints and webhooks — always accessible
        if (uri.startsWith("/api/v1/companies/subscription") || uri.startsWith("/api/v1/webhooks")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get company subscription
        User user = (User) auth.getPrincipal();
        Long userId = user.getId();

        Long companyId;
        try {
            companyId = companyService.findCompanyByUserId(userId).getId();
        } catch (Exception e) {
            log.error("SubscriptionCheckFilter: failed to resolve companyId for userId={}. Failing open.", userId, e);
            filterChain.doFilter(request, response);
            return;
        }

        Optional<CompanySubscription> subOpt = subscriptionRepository.findByCompanyId(companyId);
        if (subOpt.isEmpty()) {
            log.error("SubscriptionCheckFilter: no subscription found for companyId={}. Failing open.", companyId);
            filterChain.doFilter(request, response);
            return;
        }

        CompanySubscription sub = subOpt.get();
        if (!sub.isAccessAllowed(paddleProps.getPastDueGraceDays())) {
            writePaymentRequiredResponse(response, uri, sub.getStatus());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writePaymentRequiredResponse(
            HttpServletResponse response,
            String path,
            SubscriptionStatus status) throws IOException {

        String message = switch (status) {
            case EXPIRED   -> "Trial expired. Subscribe to continue.";
            case CANCELLED -> "Subscription cancelled. Resubscribe to continue.";
            case PAST_DUE  -> "Payment past due. Update billing details.";
            case PAUSED    -> "Subscription paused. Resume to continue.";
            default        -> "Subscription required to access this resource.";
        };

        response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED); // 402
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now(ZoneOffset.UTC).toString());
        body.put("status", 402);
        body.put("error", "Payment Required");
        body.put("message", message);
        body.put("path", path);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
