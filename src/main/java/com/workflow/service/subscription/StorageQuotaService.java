package com.workflow.service.subscription;

import com.workflow.common.exception.business.StorageLimitExceededException;
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
        // Every signup goes through initTrial(), so this shouldn't happen. Fail CLOSED to
        // FREE-tier limits (not open/unlimited) — see PlanLimitsService's Optional overloads.
        if (subscription.isEmpty()) {
            log.error("StorageQuotaService.assertCapacity: no CompanySubscription for companyId={} — treating as FREE tier for limit-checking", companyId);
        }

        long currentUsage = companyRepository.findStorageUsedBytes(companyId).orElse(0L);
        long effectiveLimit = planLimitsService.getEffectiveStorageLimitBytes(subscription);

        if (currentUsage + incomingBytes > effectiveLimit) {
            throw new StorageLimitExceededException(
                    "Storage limit reached (" + effectiveLimit + " bytes max, " + currentUsage
                            + " used). Upgrade your plan or free up space to upload more.");
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
