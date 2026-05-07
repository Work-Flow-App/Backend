package com.workflow.controller.company;

import com.workflow.common.util.AuthUtils;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.subscription.ISubscriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "Subscription")
@RestController
@RequestMapping("/api/v1/companies/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final ISubscriptionService subscriptionService;
    private final ICompanyService companyService;
    private final PaddleConfigProperties paddleProps;

    record SubscriptionStatusResponse(
            String status,
            LocalDateTime trialEndsAt,
            LocalDateTime currentPeriodEnd,
            boolean accessAllowed
    ) {}

    @GetMapping("/status")
    public ResponseEntity<SubscriptionStatusResponse> getStatus(Authentication authentication) {
        Long companyId = AuthUtils.getCompanyId(authentication, companyService);

        CompanySubscription sub = subscriptionService.getStatus(companyId);
        return ResponseEntity.ok(new SubscriptionStatusResponse(
                sub.getStatus().name(),
                sub.getTrialEndsAt(),
                sub.getCurrentPeriodEnd(),
                sub.isAccessAllowed(paddleProps.getPastDueGraceDays())
        ));
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckoutSession(Authentication authentication) {
        Long companyId = AuthUtils.getCompanyId(authentication, companyService);
        var result = subscriptionService.createCheckoutSession(companyId);
        return ResponseEntity.ok(Map.of("transactionId", result.transactionId()));
    }

    @GetMapping("/portal")
    public ResponseEntity<Map<String, String>> getPortalUrl(Authentication authentication) {
        Long companyId = AuthUtils.getCompanyId(authentication, companyService);
        return ResponseEntity.ok(Map.of("portalUrl", subscriptionService.getPortalUrl(companyId)));
    }

    @DeleteMapping
    public ResponseEntity<Void> cancelSubscription(Authentication authentication) {
        Long companyId = AuthUtils.getCompanyId(authentication, companyService);
        subscriptionService.cancelSubscription(companyId);
        return ResponseEntity.noContent().build();
    }
}
