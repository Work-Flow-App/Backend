package com.workflow.service.subscription;

import com.workflow.entity.company.CompanySubscription;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.company.CompanySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageQuotaService implements IStorageQuotaService {

    private final CompanyRepository companyRepository;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final IPlanLimitsService planLimitsService;

    @Override
    @Transactional(readOnly = true)
    public void assertCapacity(Long companyId, long incomingBytes) {
        Optional<CompanySubscription> subscription = subscriptionRepository.findByCompanyId(companyId);
        // Every signup goes through initTrial(), so this shouldn't happen — but this check is
        // tracking-only (Phase 3 adds enforcement), so a missing row must not block the upload path.
        if (subscription.isEmpty()) {
            log.warn("StorageQuotaService.assertCapacity: no CompanySubscription for companyId={} — skipping capacity check", companyId);
            return;
        }

        long currentUsage = companyRepository.findStorageUsedBytes(companyId).orElse(0L);
        long effectiveLimit = planLimitsService.getEffectiveStorageLimitBytes(subscription.get());

        if (currentUsage + incomingBytes > effectiveLimit) {
            log.warn("Storage limit exceeded (tracking only, not enforced): companyId={}, currentUsageBytes={}, incomingBytes={}, effectiveLimitBytes={}",
                    companyId, currentUsage, incomingBytes, effectiveLimit);
        }
    }

    @Override
    @Transactional
    public void recordUpload(Long companyId, long bytes) {
        companyRepository.incrementStorageUsedBytes(companyId, bytes);
    }

    @Override
    @Transactional
    public void recordDelete(Long companyId, long bytes) {
        companyRepository.decrementStorageUsedBytes(companyId, bytes);
    }
}
