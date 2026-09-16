package com.workflow.service.subscription;

import com.workflow.common.constant.PlanType;
import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.common.exception.business.SeatReductionConflictException;
import com.workflow.common.exception.business.StorageReductionConflictException;
import com.workflow.common.exception.business.SubscriptionRequiredException;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.repository.worker.WorkerInvitationRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.paddle.IPaddleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionService implements ISubscriptionService {

    private final CompanySubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;
    private final WorkerRepository workerRepository;
    private final WorkerInvitationRepository invitationRepository;
    private final IPlanLimitsService planLimitsService;
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
                .trialEndsAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(paddleProps.getTrialDays()))
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
    public ISubscriptionService.CheckoutResult createCheckoutSession(
            Long companyId, PlanType planType, int extraSeats, int extraStorageBlocks) {
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

        var session = paddleService.generateCheckoutUrl(
                sub.getPaddleCustomerId(), companyId, planType, extraSeats, extraStorageBlocks);
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

    @Override
    public ISubscriptionService.SubscriptionAddonsResult updateAddons(
            Long companyId, Integer extraSeats, Integer extraStorageBlocks) {
        // Pessimistic lock: serializes concurrent addon-update calls against each other, and against
        // SeatLimitService.assertCapacity (worker/invitation creation), which locks the same row —
        // prevents a decrease here from racing a concurrent seat consumption elsewhere.
        CompanySubscription sub = subscriptionRepository.findByCompanyIdForUpdate(companyId)
                .orElseThrow(() -> new InvalidRequestException(
                        "No subscription record found for company: " + companyId));

        if (sub.getPaddleSubscriptionId() == null) {
            throw new InvalidRequestException(
                    "Company " + companyId + " has no active Paddle subscription. Complete checkout first.");
        }
        if (sub.getPlanType() == PlanType.FREE) {
            throw new InvalidRequestException(
                    "Company " + companyId + " is on the FREE plan. Upgrade to STARTER or PROFESSIONAL to purchase addons.");
        }
        if (!sub.isMutationAllowed(paddleProps.getPastDueGraceDays())) {
            throw new SubscriptionRequiredException(
                    "Subscription for company " + companyId + " is not active. Addon purchases are unavailable.");
        }

        int newExtraSeats = extraSeats != null ? extraSeats : sub.getExtraUserSeats();
        int newExtraStorageBlocks = extraStorageBlocks != null ? extraStorageBlocks : sub.getExtraStorageBlocks();

        if (newExtraSeats == sub.getExtraUserSeats() && newExtraStorageBlocks == sub.getExtraStorageBlocks()) {
            log.info("updateAddons: no-op for companyId={} (requested values match current state)", companyId);
            return new ISubscriptionService.SubscriptionAddonsResult(
                    sub.getPlanType(), sub.getStatus(), sub.getExtraUserSeats(), sub.getExtraStorageBlocks());
        }

        if (newExtraSeats < sub.getExtraUserSeats()) {
            long inUse = workerRepository.countByCompanyIdAndArchivedFalse(companyId)
                    + invitationRepository.countPendingByCompanyId(companyId, LocalDateTime.now(ZoneOffset.UTC));
            int newEffectiveMax = planLimitsService.getEffectiveMaxUsers(sub.getPlanType(), newExtraSeats);
            if (inUse > newEffectiveMax) {
                throw new SeatReductionConflictException(
                        "Cannot reduce extra seats to " + newExtraSeats + ": " + inUse
                                + " seats currently in use, new limit would be " + newEffectiveMax + ".");
            }
        }
        if (newExtraStorageBlocks < sub.getExtraStorageBlocks()) {
            long usedBytes = companyRepository.findStorageUsedBytes(companyId).orElse(0L);
            long newEffectiveLimit = planLimitsService.getEffectiveStorageLimitBytes(sub.getPlanType(), newExtraStorageBlocks);
            if (usedBytes > newEffectiveLimit) {
                throw new StorageReductionConflictException(
                        "Cannot reduce extra storage blocks to " + newExtraStorageBlocks + ": " + usedBytes
                                + " bytes currently used, new limit would be " + newEffectiveLimit + " bytes.");
            }
        }

        // Paddle call first — only persist locally once Paddle has confirmed the change. If this
        // throws, the transaction rolls back and nothing is written (the lock is simply released).
        paddleService.updateSubscriptionAddons(
                sub.getPaddleSubscriptionId(), sub.getPlanType(), newExtraSeats, newExtraStorageBlocks);

        // Optimistic local write (not gated by the webhook's lastEventOccurredAt watermark — see
        // CompanySubscriptionRepository.updateAddonsOptimistic javadoc). The subscription.updated
        // webhook Paddle sends for this same PATCH will land afterward and reconcile/confirm.
        subscriptionRepository.updateAddonsOptimistic(sub.getId(), newExtraSeats, newExtraStorageBlocks);

        log.info("Addons updated for companyId={}: extraSeats={}, extraStorageBlocks={}",
                companyId, newExtraSeats, newExtraStorageBlocks);

        return new ISubscriptionService.SubscriptionAddonsResult(
                sub.getPlanType(), sub.getStatus(), newExtraSeats, newExtraStorageBlocks);
    }
}
