package com.workflow.service.asset;

import com.workflow.dto.asset.*;
import com.workflow.entity.*;
import com.workflow.repository.*;
import com.workflow.common.exception.business.*;
import com.workflow.service.sequence.CompanyCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
// import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssetService implements IAssetService {

    private final AssetRepository assetRepository;
    private final AssetJobAssignmentRepository assignmentRepository;
    private final CompanyRepository companyRepository;
    private final CompanyCounterService companyCounterService;
    // private final JobRepository jobRepository;
    // private final WorkerRepository workerRepository;

    // ----------------- CREATE --------------------
    @Override
    public AssetResponse createAsset(AssetCreateRequest request, Long companyId) {
        log.info("Creating asset: name={}, assetTag={}, purchasePrice={}, companyId={}",
                 request.getName(), request.getAssetTag(), request.getPurchasePrice(), companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> {
                    log.error("Company not found when creating asset: companyId={}", companyId);
                    return new CompanyNotFoundException("Company not found");
                });

        // validations
        if (request.getName() == null || request.getName().trim().length() < 2 || request.getName().length() > 150) {
            throw new IllegalArgumentException("Asset name is required (2-150 chars)");
        }
        if (assetRepository.existsByCompanyIdAndName(companyId, request.getName())) {
            throw new DuplicateNameException("Asset name must be unique within the company");
        }
        if (request.getAssetTag() != null
                && assetRepository.existsByCompanyIdAndAssetTag(companyId, request.getAssetTag())) {
            throw new DuplicateNameException("Asset tag must be unique within the company");
        }
        if (request.getPurchasePrice() == null || request.getPurchasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Purchase price must be greater than 0");
        }
        if (request.getPurchaseDate() == null || request.getPurchaseDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Purchase date is required and cannot be in the future");
        }
        if (request.getDepreciationRate() == null
                || request.getDepreciationRate().compareTo(BigDecimal.ZERO) < 0
                || request.getDepreciationRate().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Depreciation rate must be between 0 and 100");
        }
        if (request.getSalvageValue() != null && request.getPurchasePrice().compareTo(request.getSalvageValue()) < 0) {
            throw new IllegalArgumentException("Purchase price must be greater than salvage value");
        }

        Asset asset = Asset.builder()
                .company(company)
                .name(request.getName())
                .description(request.getDescription())
                .serialNumber(request.getSerialNumber())
                .assetTag(request.getAssetTag())
                .purchasePrice(request.getPurchasePrice().setScale(2, RoundingMode.HALF_UP))
                .purchaseDate(request.getPurchaseDate())
                .depreciationRate(request.getDepreciationRate().setScale(2, RoundingMode.HALF_UP))
                .salvageValue(request.getSalvageValue() == null ? BigDecimal.ZERO
                        : request.getSalvageValue().setScale(2, RoundingMode.HALF_UP))
                .available(true)
                .archived(false)
                .assetRef(companyCounterService.nextAssetId(companyId))
                .build();

        assetRepository.save(asset);
        return mapToResponse(asset);
    }

    // ----------------- UPDATE --------------------
    @Override
    public AssetResponse updateAsset(Long assetId, AssetUpdateRequest request, Long companyId) {
        Asset asset = assetRepository.findById(assetId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));

        if (asset.isArchived()) {
            throw new IllegalStateException("Cannot update archived asset");
        }

        if (request.getName() != null && !request.getName().equals(asset.getName())) {
            if (assetRepository.existsByCompanyIdAndName(companyId, request.getName())) {
                throw new DuplicateNameException("Asset name must be unique within the company");
            }
            asset.setName(request.getName());
        }

        if (request.getAssetTag() != null && !Objects.equals(request.getAssetTag(), asset.getAssetTag())) {
            if (assetRepository.existsByCompanyIdAndAssetTag(companyId, request.getAssetTag())) {
                throw new DuplicateNameException("Asset tag must be unique within the company");
            }
            asset.setAssetTag(request.getAssetTag());
        }

        asset.setDescription(request.getDescription());
        asset.setSerialNumber(request.getSerialNumber());
        if (request.getSalvageValue() != null) {
            if (asset.getPurchasePrice().compareTo(request.getSalvageValue()) < 0) {
                throw new IllegalArgumentException("Purchase price must be greater than salvage value");
            }
            asset.setSalvageValue(request.getSalvageValue().setScale(2, RoundingMode.HALF_UP));
        }

        assetRepository.save(asset);
        return mapToResponse(asset);
    }

    // ----------------- GET / LIST --------------------
    @Override
    public AssetResponse getAsset(Long assetId, Long companyId) {
        Asset asset = assetRepository.findById(assetId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));
        return mapToResponse(asset);
    }

    @Override
    public Page<AssetResponse> listAssets(Long companyId, int page, int size, Boolean archived, Boolean available,
            String sortBy, String direction) {
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sort = (sortBy == null || sortBy.isBlank()) ? "name" : sortBy;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(dir, sort));

        Page<Asset> assetsPage;
        if (archived == null && available == null) {
            assetsPage = assetRepository.findByCompanyIdAndArchivedFalse(companyId, pageable);
        } else if (archived != null) {
            if (archived)
                assetsPage = assetRepository.findByCompanyId(companyId, pageable);
            else
                assetsPage = assetRepository.findByCompanyIdAndArchivedFalse(companyId, pageable);
        } else { // available filter provided
            assetsPage = assetRepository.findByCompanyIdAndArchivedFalseAndAvailable(companyId, available, pageable);
        }

        return assetsPage.map(this::mapToResponse);
    }

    // ----------------- ARCHIVE --------------------
    @Override
    public void archiveAsset(Long assetId, Long companyId) {
        Asset asset = assetRepository.findById(assetId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));

        // cannot archive if currently assigned to active job
        Optional<AssetJobAssignment> active = assignmentRepository.findByAssetIdAndReturnedAtIsNull(assetId);
        if (active.isPresent()) {
            throw new IllegalStateException("Cannot archive asset currently assigned to active job");
        }
        asset.setArchived(true);
        assetRepository.save(asset);
    }

    // ----------------- DEPRECIATION --------------------
    @Override
    public AssetValueResponse calculateAssetValue(Long assetId, Long companyId, LocalDate asOfDate) {
        Asset asset = assetRepository.findById(assetId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));

        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        if (date.isBefore(asset.getPurchaseDate())) {
            // per business rules, if as-of date before purchase date return purchase price
            return AssetValueResponse.builder()
                    .assetId(asset.getId())
                    .assetName(asset.getName())
                    .purchasePrice(asset.getPurchasePrice())
                    .currentValue(asset.getPurchasePrice())
                    .totalDepreciation(BigDecimal.ZERO)
                    .depreciationRate(asset.getDepreciationRate())
                    .salvageValue(asset.getSalvageValue())
                    .purchaseDate(asset.getPurchaseDate())
                    .daysOwned(0)
                    .yearsOwned(0.0)
                    .valueAsOfDate(date.atStartOfDay())
                    .build();
        }

        long daysOwned = Duration.between(asset.getPurchaseDate().atStartOfDay(), date.atStartOfDay()).toDays();
        double yearsOwned = daysOwned / 365.25;

        // rate as fraction
        BigDecimal ratePercent = asset.getDepreciationRate();
        double r = ratePercent.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP).doubleValue();

        // Declining balance formula: current = purchase * (1 - r)^years
        double factor = Math.pow(1.0 - r, yearsOwned);

        BigDecimal currentValue = asset.getPurchasePrice().multiply(new BigDecimal(factor));
        // never less than salvage value (or 0 if not set)
        BigDecimal salvage = asset.getSalvageValue() == null ? BigDecimal.ZERO : asset.getSalvageValue();
        if (currentValue.compareTo(salvage) < 0)
            currentValue = salvage;

        currentValue = currentValue.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDepreciation = asset.getPurchasePrice().subtract(currentValue).max(BigDecimal.ZERO).setScale(2,
                RoundingMode.HALF_UP);

        return AssetValueResponse.builder()
                .assetId(asset.getId())
                .assetName(asset.getName())
                .purchasePrice(asset.getPurchasePrice())
                .currentValue(currentValue)
                .totalDepreciation(totalDepreciation)
                .depreciationRate(asset.getDepreciationRate())
                .salvageValue(salvage)
                .purchaseDate(asset.getPurchaseDate())
                .daysOwned(daysOwned)
                .yearsOwned(Math.round(yearsOwned * 100.0) / 100.0)
                .valueAsOfDate(date.atStartOfDay())
                .build();
    }

    // ----------------- DASHBOARD --------------------
    @Override
    public AssetStatistics getStatistics(Long companyId) {
        // Use aggregate queries for counts — avoids loading all assets into heap
        long total = assetRepository.countActiveByCompanyId(companyId);
        long available = assetRepository.countAvailableByCompanyId(companyId);
        long inUse = total - available;

        // Depreciation calculations require per-asset data; load only active assets
        // (not archived) without Pageable.unpaged() — use targeted query instead
        List<Asset> assets = assetRepository.findActiveByCompanyId(companyId);

        LocalDate today = LocalDate.now();
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;
        double totalRate = 0;

        for (Asset asset : assets) {
            totalPurchase = totalPurchase.add(asset.getPurchasePrice());
            totalCurrent = totalCurrent.add(calculateCurrentValue(asset, today));
            totalRate += asset.getDepreciationRate().doubleValue();
        }

        BigDecimal totalDep = totalPurchase.subtract(totalCurrent).max(BigDecimal.ZERO);
        double avgRate = total > 0 ? totalRate / total : 0.0;

        return AssetStatistics.builder()
                .totalAssets(total)
                .availableAssets(available)
                .assetsInUse(inUse)
                .totalPurchaseValue(totalPurchase.setScale(2, RoundingMode.HALF_UP))
                .totalCurrentValue(totalCurrent.setScale(2, RoundingMode.HALF_UP))
                .totalDepreciation(totalDep.setScale(2, RoundingMode.HALF_UP))
                .averageDepreciationRate(Math.round(avgRate * 100.0) / 100.0)
                .build();
    }

    // ----------------- HELPERS --------------------
    /**
     * Calculate current depreciated value of an asset without making database queries.
     * Used internally for efficient batch calculations (e.g., in getStatistics).
     */
    private BigDecimal calculateCurrentValue(Asset asset, LocalDate asOfDate) {
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;

        // If date is before purchase, value equals purchase price
        if (date.isBefore(asset.getPurchaseDate())) {
            return asset.getPurchasePrice();
        }

        long daysOwned = Duration.between(asset.getPurchaseDate().atStartOfDay(), date.atStartOfDay()).toDays();
        double yearsOwned = daysOwned / 365.25;

        // Convert depreciation rate from percentage to fraction
        double rate = asset.getDepreciationRate().divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP).doubleValue();

        // Declining balance formula: current = purchase * (1 - rate)^years
        double depreciationFactor = Math.pow(1.0 - rate, yearsOwned);
        BigDecimal currentValue = asset.getPurchasePrice().multiply(new BigDecimal(depreciationFactor));

        // Never go below salvage value
        BigDecimal salvageValue = asset.getSalvageValue() == null ? BigDecimal.ZERO : asset.getSalvageValue();
        if (currentValue.compareTo(salvageValue) < 0) {
            currentValue = salvageValue;
        }

        return currentValue.setScale(2, RoundingMode.HALF_UP);
    }

    // ----------------- MAPPERS --------------------
    private AssetResponse mapToResponse(Asset asset) {
        return AssetResponse.builder()
                .id(asset.getId())
                .assetRef(asset.getAssetRef())
                .companyId(asset.getCompany().getId())
                .name(asset.getName())
                .description(asset.getDescription())
                .serialNumber(asset.getSerialNumber())
                .assetTag(asset.getAssetTag())
                .purchasePrice(asset.getPurchasePrice())
                .purchaseDate(asset.getPurchaseDate())
                .depreciationRate(asset.getDepreciationRate())
                .salvageValue(asset.getSalvageValue())
                .available(asset.isAvailable())
                .archived(asset.isArchived())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}
