package com.workflow.service.company;

import com.workflow.common.exception.business.CompanyAlreadyExistsException;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.dto.company.CompanyAddressRequest;
import com.workflow.dto.company.CompanyBankDetailsRequest;
import com.workflow.dto.company.CompanyDashboardResponse;
import com.workflow.dto.company.CompanyProfileResponse;
import com.workflow.dto.company.CompanyProfileUpdateRequest;
import com.workflow.dto.company.UsageSummaryResponse;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanyAddress;
import com.workflow.entity.company.CompanyBankDetails;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.service.storage.IStorageService;
import com.workflow.service.subscription.IPlanLimitsService;
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
public class CompanyService implements ICompanyService {

    // 80% of the effective limit — matches the business requirement to warn before the Phase 3 hard block
    private static final double USAGE_WARNING_THRESHOLD = 0.8;

    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final IPlanLimitsService planLimitsService;
    private final IStorageService s3Service;

    @Override
    @Transactional
    public CompanyProfileResponse updateProfile(CompanyProfileUpdateRequest request, Long userId) {
        Company company = findCompanyByUserId(userId);

        // Check if name is being changed and if it already exists
        if (!company.getName().equalsIgnoreCase(request.name()) &&
                companyRepository.existsByNameIgnoreCase(request.name())) {
            throw new CompanyAlreadyExistsException("Company with name '" + request.name() + "' already exists");
        }

        // Update company fields
        company.setName(request.name());

        company.setDescription(request.description());
        company.setWebsite(request.website());
        company.setTagline(request.tagline());

        if (request.address() != null) {
            CompanyAddressRequest a = request.address();
            company.setAddress(CompanyAddress.builder()
                    .addressLine1(a.addressLine1())
                    .addressLine2(a.addressLine2())
                    .addressLine3(a.addressLine3())
                    .town(a.town())
                    .country(a.country())
                    .postcode(a.postcode())
                    .build());
        }
        company.setTelephone(request.telephone());
        company.setMobile(request.mobile());
        company.setFax(request.fax());
        company.setEmail(request.email());
        company.setContactEmail(request.contactEmail());
        company.setContactNumber(request.contactNumber());
        company.setVatNumber(request.vatNumber());
        if (request.currency() != null) {
            company.setCurrency(request.currency());
        }

        if (request.bankDetails() != null) {
            CompanyBankDetailsRequest bd = request.bankDetails();
            CompanyBankDetails bankDetails = company.getBankDetails();
            if (bankDetails == null) {
                bankDetails = CompanyBankDetails.builder().company(company).build();
                company.setBankDetails(bankDetails);
            }
            bankDetails.setBankName(bd.bankName());
            bankDetails.setAccountName(bd.accountName());
            bankDetails.setAccountNo(bd.accountNo());
            bankDetails.setSortCode(bd.sortCode());
        }

        Company savedCompany = companyRepository.save(company);
        // Resolve logo URL using S3 service if it exists
        String resolvedLogoUrl = savedCompany.getLogoUrl() != null ? s3Service.resolveFileUrl(savedCompany.getLogoUrl())
                : null;

        return CompanyProfileResponse.fromEntity(savedCompany, resolvedLogoUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileResponse getProfile(Long userId) {
        Company company = findCompanyByUserId(userId);

        // Resolve the logo URL before returning
        String resolvedLogoUrl = company.getLogoUrl() != null ? s3Service.resolveFileUrl(company.getLogoUrl()) : null;

        return CompanyProfileResponse.fromEntity(company, resolvedLogoUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyDashboardResponse getDashboard(Long userId) {
        Company company = findCompanyByUserId(userId);

        long totalWorkers = companyRepository.countWorkers(company.getId());
        long activeWorkers = companyRepository.countActiveWorkers(company.getId());
        long archivedWorkers = totalWorkers - activeWorkers;
        long totalClients = companyRepository.countClients(company.getId());

        return new CompanyDashboardResponse(
                company.getId(),
                company.getName(),
                totalWorkers,
                totalClients,
                activeWorkers,
                archivedWorkers,
                buildUsageSummary(company, activeWorkers));
    }

    @Override
    @Transactional(readOnly = true)
    public UsageSummaryResponse getUsageSummary(Long userId) {
        Company company = findCompanyByUserId(userId);
        long activeWorkers = companyRepository.countActiveWorkers(company.getId());
        return buildUsageSummary(company, activeWorkers);
    }

    // Never throws and never returns null on a missing CompanySubscription — fails CLOSED to
    // FREE-tier limits instead (see PlanLimitsService's Optional overloads), same as the other 3
    // enforcement points (job/seat/storage caps). This should never legitimately happen (every
    // signup gets a CompanySubscription row), hence the loud ERROR log.
    private UsageSummaryResponse buildUsageSummary(Company company, long activeWorkers) {
        Long companyId = company.getId();

        Optional<CompanySubscription> subscription = subscriptionRepository.findByCompanyId(companyId);
        if (subscription.isEmpty()) {
            log.error("buildUsageSummary: no CompanySubscription for companyId={} — treating as FREE tier for limit-checking", companyId);
        }

        LocalDateTime startOfMonth = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);
        long jobsUsed = jobRepository.countByCompanyIdAndCreatedAtBetween(companyId, startOfMonth, startOfNextMonth);
        int jobsLimit = planLimitsService.getEffectiveJobsPerMonth(subscription);

        long storageUsed = company.getStorageUsedBytes();
        long storageLimit = planLimitsService.getEffectiveStorageLimitBytes(subscription);

        int seatsLimit = planLimitsService.getEffectiveMaxUsers(subscription);

        return new UsageSummaryResponse(
                jobsUsed, jobsLimit, isAtOrAboveWarningThreshold(jobsUsed, jobsLimit),
                storageUsed, storageLimit, isAtOrAboveWarningThreshold(storageUsed, storageLimit),
                activeWorkers, seatsLimit, isAtOrAboveWarningThreshold(activeWorkers, seatsLimit));
    }

    private boolean isAtOrAboveWarningThreshold(long used, long limit) {
        return limit > 0 && used >= limit * USAGE_WARNING_THRESHOLD;
    }

    @Override
    @Transactional(readOnly = true)
    public Company findCompanyByUserId(Long userId) {
        return companyRepository.findByUserIdAndNotArchived(userId)
                .orElseThrow(() -> new CompanyNotFoundException("No active company found for this user"));
    }
}