package com.workflow.service.subscription;

import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.service.paddle.IPaddleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionService implements ISubscriptionService {

    private final CompanySubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;
    private final IPaddleService paddleService;
    private final PaddleConfigProperties paddleProps;

    @Override
    public void initTrial(Long companyId) {
        // Guard against duplicate trial init (e.g. retry or double-call)
        if (subscriptionRepository.findByCompanyId(companyId).isPresent()) {
            log.warn("Trial already initialized for companyId={}. Skipping.", companyId);
            return;
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found: " + companyId));

        CompanySubscription subscription = CompanySubscription.builder()
                .company(company)
                .status(SubscriptionStatus.TRIAL)
                .trialEndsAt(LocalDateTime.now().plusDays(paddleProps.getTrialDays()))
                .build();

        subscriptionRepository.save(subscription);
        log.info("Trial initialized for companyId={}, expiresAt={}", companyId, subscription.getTrialEndsAt());
    }

    @Override
    @Transactional(readOnly = true)
    public CompanySubscription getStatus(Long companyId) {
        return subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new InvalidRequestException(
                        "No subscription found for company: " + companyId));
    }

    @Override
    public ISubscriptionService.CheckoutResult createCheckoutSession(Long companyId) {
        CompanySubscription sub = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new InvalidRequestException(
                        "No subscription record found for company: " + companyId));

        if (sub.getPaddleCustomerId() == null) {
            Company company = sub.getCompany();
            var customerResponse = paddleService.createCustomer(company.getEmail(), company.getName());
            sub.setPaddleCustomerId(customerResponse.data().id());
            subscriptionRepository.save(sub);
            log.info("Created Paddle customer id={} for companyId={}", sub.getPaddleCustomerId(), companyId);
        }

        var session = paddleService.generateCheckoutUrl(sub.getPaddleCustomerId(), companyId);
        String txnId = session.data().id();
        String checkoutUrl = session.data().checkout() != null ? session.data().checkout().url() : null;
        log.info("Checkout session created for companyId={}, txnId={}", companyId, txnId);
        return new ISubscriptionService.CheckoutResult(txnId, checkoutUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public String getPortalUrl(Long companyId) {
        CompanySubscription sub = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new InvalidRequestException(
                        "No subscription record found for company: " + companyId));

        if (sub.getPaddleCustomerId() == null) {
            throw new InvalidRequestException(
                    "No Paddle customer exists yet for company: " + companyId
                    + ". Complete checkout first.");
        }

        var response = paddleService.generatePortalUrl(sub.getPaddleCustomerId());
        return response.data().urls().general().overview();
    }

    @Override
    public void cancelSubscription(Long companyId) {
        CompanySubscription sub = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new InvalidRequestException(
                        "No subscription record found for company: " + companyId));

        if (sub.getPaddleSubscriptionId() == null) {
            throw new InvalidRequestException(
                    "Company " + companyId + " has no active Paddle subscription to cancel.");
        }

        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new InvalidRequestException(
                    "Subscription for company " + companyId + " is already cancelled.");
        }

        paddleService.cancelSubscription(sub.getPaddleSubscriptionId());
        // Status change (CANCELLED) will arrive via webhook — we do NOT update status here
        log.info("Cancellation requested for companyId={}, paddleSubId={}",
                companyId, sub.getPaddleSubscriptionId());
    }
}
