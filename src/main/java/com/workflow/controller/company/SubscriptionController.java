package com.workflow.controller.company;

import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.service.subscription.ISubscriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Subscription")
@RestController
@RequestMapping("/api/v1/companies/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final ISubscriptionService subscriptionService;
    private final PaddleConfigProperties paddleProps;

    record SubscriptionStatusResponse(
            SubscriptionStatus status,
            LocalDateTime trialEndsAt,
            LocalDateTime currentPeriodEnd,
            boolean accessAllowed
    ) {}

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/status")
    public ResponseEntity<SubscriptionStatusResponse> getStatus(Authentication authentication) {
        CompanySubscription sub = subscriptionService.getStatus(AuthUtils.getCompanyId());
        return ResponseEntity.ok(new SubscriptionStatusResponse(
                sub.getStatus(),
                sub.getTrialEndsAt(),
                sub.getCurrentPeriodEnd(),
                sub.isAccessAllowed(paddleProps.getPastDueGraceDays())
        ));
    }

    @RequireCompanyRole({COMPANY_ADMIN})
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckoutSession(Authentication authentication) {
        var result = subscriptionService.createCheckoutSession(AuthUtils.getCompanyId());
        return ResponseEntity.ok(Map.of("transactionId", result.transactionId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN})
    @GetMapping("/portal")
    public ResponseEntity<Map<String, String>> getPortalUrl(Authentication authentication) {
        return ResponseEntity.ok(Map.of("portalUrl", subscriptionService.getPortalUrl(AuthUtils.getCompanyId())));
    }

    @RequireCompanyRole({COMPANY_ADMIN})
    @DeleteMapping
    public ResponseEntity<Void> cancelSubscription(Authentication authentication) {
        subscriptionService.cancelSubscription(AuthUtils.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
