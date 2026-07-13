package com.workflow.service.asset;

import com.workflow.dto.asset.*;
import com.workflow.dto.job.AddressRequest;
import com.workflow.dto.job.AddressResponse;
import com.workflow.entity.asset.Asset;
import com.workflow.entity.asset.AssetAttachment;
import com.workflow.entity.asset.AssetJobAssignment;
import com.workflow.entity.common.Address;
import com.workflow.entity.company.Company;
import com.workflow.repository.asset.AssetJobAssignmentRepository;
import com.workflow.repository.asset.AssetRepository;
import com.workflow.repository.common.AddressRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.common.constant.asset.AssetLocationType;
import com.workflow.common.exception.business.*;
import com.workflow.service.sequence.CompanyCounterService;
import com.workflow.service.storage.IStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.tika.Tika;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssetService implements IAssetService {

    private final AssetRepository assetRepository;
    private final AssetJobAssignmentRepository assignmentRepository;
    private final CompanyRepository companyRepository;
    private final CompanyCounterService companyCounterService;
    private final AddressRepository addressRepository;
    private final IStorageService s3Service;
    private final Tika tika;

    @Override
    public AssetResponse createAsset(AssetCreateRequest request, Long companyId) {
        log.info("Creating asset: name={}, assetTag={}, purchasePrice={}, companyId={}",
                request.getName(), request.getAssetTag(), request.getPurchasePrice(), companyId);

        Company company = companyRepository.getReferenceById(companyId);

        if (assetRepository.existsByCompanyIdAndName(companyId, request.getName())) {
            throw new DuplicateNameException("Asset name must be unique within the company");
        }
        if (request.getAssetTag() != null
                && assetRepository.existsByCompanyIdAndAssetTag(companyId, request.getAssetTag())) {
            throw new DuplicateNameException("Asset tag must be unique within the company");
        }
        if (request.getSalvageValue() != null && request.getPurchasePrice().compareTo(request.getSalvageValue()) < 0) {
            throw new IllegalArgumentException("Purchase price must be greater than salvage value");
        }

        Address warehouseAddress = mapToAddress(request.getWarehouseAddress());
        if (warehouseAddress != null) {
            warehouseAddress = addressRepository.save(warehouseAddress);
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
                .locationType(AssetLocationType.WAREHOUSE)
                .warehouseAddress(warehouseAddress)
                .address(warehouseAddress)
                .build();

        assetRepository.save(asset);
        return mapToResponse(asset);
    }

    @Override
    public AssetResponse updateAsset(Long assetId, AssetUpdateRequest request, Long companyId) {
        Asset asset = assetRepository.findById(assetId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));

        if (asset.isArchived()) {
            throw new InvalidRequestException("Cannot update archived asset");
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

        if (request.getDescription() != null) {
            asset.setDescription(request.getDescription());
        }
        if (request.getSerialNumber() != null) {
            asset.setSerialNumber(request.getSerialNumber());
        }

        // --- NEW: Update Financial & Lifecycle Fields ---
        if (request.getPurchasePrice() != null) {
            asset.setPurchasePrice(request.getPurchasePrice().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getPurchaseDate() != null) {
            asset.setPurchaseDate(request.getPurchaseDate());
        }
        if (request.getDepreciationRate() != null) {
            asset.setDepreciationRate(request.getDepreciationRate().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getSalvageValue() != null) {
            asset.setSalvageValue(request.getSalvageValue().setScale(2, RoundingMode.HALF_UP));
        }

        // Holistic Cross-Field Validation: Ensure final salvage doesn't exceed final
        // purchase price
        BigDecimal finalPurchasePrice = asset.getPurchasePrice();
        BigDecimal finalSalvageValue = asset.getSalvageValue() == null ? BigDecimal.ZERO : asset.getSalvageValue();
        if (finalPurchasePrice.compareTo(finalSalvageValue) < 0) {
            throw new InvalidRequestException("Purchase price must be greater than or equal to salvage value");
        }

        // Handle Warehouse Address Update
        if (request.getWarehouseAddress() != null) {
            Address updatedWarehouse = mapToAddress(request.getWarehouseAddress());
            updatedWarehouse = addressRepository.save(updatedWarehouse);
            asset.setWarehouseAddress(updatedWarehouse);

            // If the asset is currently at the warehouse, update its current location too
            if (asset.getLocationType() == AssetLocationType.WAREHOUSE) {
                asset.setAddress(updatedWarehouse);
            }
        }

        assetRepository.save(asset);
        return mapToResponse(asset);
    }

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
        } else {
            assetsPage = assetRepository.findByCompanyIdAndArchivedFalseAndAvailable(companyId, available, pageable);
        }

        return assetsPage.map(this::mapToResponse);
    }

    @Override
    public void archiveAsset(Long assetId, Long companyId) {
        Asset asset = assetRepository.findById(assetId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));

        Optional<AssetJobAssignment> active = assignmentRepository.findByAssetIdAndReturnedAtIsNull(assetId);
        if (active.isPresent()) {
            throw new IllegalStateException("Cannot archive asset currently assigned to active job");
        }
        asset.setArchived(true);
        assetRepository.save(asset);
    }

    @Override
    public AssetValueResponse calculateAssetValue(Long assetId, Long companyId, LocalDate asOfDate) {
        Asset asset = assetRepository.findById(assetId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));

        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        if (date.isBefore(asset.getPurchaseDate())) {
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

        BigDecimal ratePercent = asset.getDepreciationRate();
        double r = ratePercent.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP).doubleValue();

        double factor = Math.pow(1.0 - r, yearsOwned);

        BigDecimal currentValue = asset.getPurchasePrice().multiply(new BigDecimal(factor));
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

    @Override
    public AssetStatistics getStatistics(Long companyId) {
        long total = assetRepository.countActiveByCompanyId(companyId);
        long available = assetRepository.countAvailableByCompanyId(companyId);
        long inUse = total - available;

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

    private BigDecimal calculateCurrentValue(Asset asset, LocalDate asOfDate) {
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;

        if (date.isBefore(asset.getPurchaseDate())) {
            return asset.getPurchasePrice();
        }

        long daysOwned = Duration.between(asset.getPurchaseDate().atStartOfDay(), date.atStartOfDay()).toDays();
        double yearsOwned = daysOwned / 365.25;

        double rate = asset.getDepreciationRate().divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP).doubleValue();

        double depreciationFactor = Math.pow(1.0 - rate, yearsOwned);
        BigDecimal currentValue = asset.getPurchasePrice().multiply(new BigDecimal(depreciationFactor));

        BigDecimal salvageValue = asset.getSalvageValue() == null ? BigDecimal.ZERO : asset.getSalvageValue();
        if (currentValue.compareTo(salvageValue) < 0) {
            currentValue = salvageValue;
        }

        return currentValue.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public AssetResponse addAttachments(Long assetId, List<MultipartFile> files, Long companyId) {
        Asset asset = assetRepository.findById(assetId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty())
                    continue;

                try {
                    String detectedType = tika.detect(file.getInputStream());
                    String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
                    String extension = originalFilename.contains(".")
                            ? originalFilename.substring(originalFilename.lastIndexOf("."))
                            : "";
                    String safeUniqueFilename = UUID.randomUUID().toString() + extension;

                    String key = String.format("companies/%d/assets/%d/%s", companyId, assetId, safeUniqueFilename);

                    s3Service.upload(key, file.getInputStream(), file.getSize(), detectedType);

                    // APPEND to the list, do NOT clear it
                    asset.getAttachments().add(AssetAttachment.builder()
                            .fileName(originalFilename)
                            .fileType(detectedType)
                            .fileUrl(key)
                            .build());

                } catch (Exception e) {
                    throw new InvalidRequestException("Failed to process file upload: " + e.getMessage());
                }
            }
            assetRepository.save(asset);
        }

        return mapToResponse(asset);
    }

    @Override
    public AssetResponse removeAttachment(Long assetId, String fileUrl, Long companyId) {
        Asset asset = assetRepository.findById(assetId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));

        // Find and remove the attachment from the Java list
        // removeIf returns true if an item was actually removed
        boolean removed = asset.getAttachments().removeIf(att -> att.getFileUrl().equals(fileUrl));

        if (removed) {
            // Delete from S3
            s3Service.delete(fileUrl);

            // Saving the asset will automatically delete the row in the asset_attachments
            // DB table
            // because it is an @ElementCollection
            assetRepository.save(asset);
        }

        return mapToResponse(asset);
    }

    private Address mapToAddress(AddressRequest ar) {
        if (ar == null)
            return null;
        return Address.builder()
                .street(ar.getStreet())
                .city(ar.getCity())
                .state(ar.getState())
                .postalCode(ar.getPostalCode())
                .country(ar.getCountry())
                .additionalInfo(ar.getAdditionalInfo())
                .latitude(ar.getLatitude())
                .longitude(ar.getLongitude())
                .build();
    }

    private AddressResponse mapAddressToResponse(Address a) {
        if (a == null)
            return null;
        return AddressResponse.builder()
                .id(a.getId())
                .street(a.getStreet())
                .city(a.getCity())
                .state(a.getState())
                .postalCode(a.getPostalCode())
                .country(a.getCountry())
                .additionalInfo(a.getAdditionalInfo())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .build();
    }

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
                .locationType(asset.getLocationType())
                .address(mapAddressToResponse(asset.getAddress()))
                .warehouseAddress(mapAddressToResponse(asset.getWarehouseAddress()))
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .attachments(asset.getAttachments() != null ? asset.getAttachments().stream()
                        .map(a -> AssetAttachmentDto.builder()
                                .fileName(a.getFileName())
                                .fileType(a.getFileType())
                                .fileUrl(s3Service.resolveFileUrl(a.getFileUrl()))
                                .build())
                        .toList() : Collections.emptyList())
                .build();
    }
}