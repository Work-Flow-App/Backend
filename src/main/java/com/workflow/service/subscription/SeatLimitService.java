package com.workflow.service.subscription;

import com.workflow.common.exception.business.SeatLimitExceededException;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.repository.worker.WorkerInvitationRepository;
import com.workflow.repository.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLimitService implements ISeatLimitService {

    private final WorkerRepository workerRepository;
    private final WorkerInvitationRepository invitationRepository;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final IPlanLimitsService planLimitsService;

    // Not readOnly: findByCompanyIdForUpdate acquires a pessimistic write lock, which some JDBC
    // configurations reject inside a read-only transaction. Callers (WorkerService.createWorker,
    // WorkerInvitationService.createInvitation) are already @Transactional, so this joins their
    // transaction — the lock is held until THAT transaction commits, i.e. through the eventual
    // insert, which is what actually serializes concurrent seat-limit checks.
    @Override
    @Transactional
    public void assertCapacity(Long companyId) {
        Optional<CompanySubscription> subscription = subscriptionRepository.findByCompanyIdForUpdate(companyId);
        // Every signup goes through initTrial(), so this shouldn't happen. Fail CLOSED to FREE-tier
        // limits (not open/unlimited) — see PlanLimitsService's Optional overloads. Note there's no
        // row to lock in this branch, so the race window below isn't closed for this specific edge case.
        if (subscription.isEmpty()) {
            log.error("SeatLimitService.assertCapacity: no CompanySubscription for companyId={} — treating as FREE tier for limit-checking", companyId);
        }

        long activeWorkers = workerRepository.countByCompanyIdAndArchivedFalse(companyId);
        long pendingInvitations = invitationRepository.countPendingByCompanyId(companyId, LocalDateTime.now(ZoneOffset.UTC));
        int effectiveMaxUsers = planLimitsService.getEffectiveMaxUsers(subscription);

        if (activeWorkers + pendingInvitations >= effectiveMaxUsers) {
            throw new SeatLimitExceededException(
                    "Seat limit reached (" + effectiveMaxUsers + " max). Upgrade your plan or remove inactive workers to add more.");
        }
    }
}
