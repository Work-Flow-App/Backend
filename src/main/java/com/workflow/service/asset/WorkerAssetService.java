package com.workflow.service.asset;

import com.workflow.common.constant.asset.AssetLocationType;
import com.workflow.common.exception.business.AssetAssignmentNotFoundException;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.dto.asset.AssetAssignmentResponse;
import com.workflow.dto.asset.AssetAttachmentDto;
import com.workflow.dto.asset.AssetResponse;
import com.workflow.dto.job.AddressRequest;
import com.workflow.dto.job.AddressResponse;
import com.workflow.entity.asset.Asset;
import com.workflow.entity.asset.AssetJobAssignment;
import com.workflow.entity.common.Address;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.asset.AssetJobAssignmentRepository;
import com.workflow.repository.asset.AssetRepository;
import com.workflow.repository.common.AddressRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.storage.IStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkerAssetService implements IWorkerAssetService {

    private final WorkerRepository workerRepository;
    private final AssetJobAssignmentRepository assignmentRepository;
    private final AssetRepository assetRepository;
    private final AddressRepository addressRepository;
    private final IStorageService s3Service;

    private Worker getWorker(Long userId) {
        return workerRepository.findByUserId(userId)
                .orElseThrow(() -> new WorkerNotFoundException("Current user is not a registered worker"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetAssignmentResponse> getMyAssignedAssets(Long workerUserId) {
        Worker worker = getWorker(workerUserId);

        return assignmentRepository.findByAssignedWorkerIdAndReturnedAtIsNull(worker.getId())
                .stream()
                .map(this::mapAssignmentToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getAssignedAssetDetails(Long assetId, Long workerUserId) {
        Worker worker = getWorker(workerUserId);

        // Verify the worker is currently assigned to this asset
        AssetJobAssignment assignment = assignmentRepository
                .findByAssetIdAndAssignedWorkerIdAndReturnedAtIsNull(assetId, worker.getId())
                .orElseThrow(() -> new ForbiddenActionException("You are not currently assigned to this asset."));

        return mapAssetToResponse(assignment.getAsset());
    }

    @Override
    public AssetAssignmentResponse updateAssetAddress(Long assignmentId, AddressRequest addressRequest,
            Long workerUserId) {
        Worker worker = getWorker(workerUserId);

        AssetJobAssignment assignment = assignmentRepository
                .findByIdAndAssignedWorkerIdAndReturnedAtIsNull(assignmentId, worker.getId())
                .orElseThrow(() -> new AssetAssignmentNotFoundException("Active assignment not found for this worker"));

        // Save the new address
        Address newAddress = null;
        if (addressRequest != null) {
            newAddress = Address.builder()
                    .street(addressRequest.getStreet())
                    .city(addressRequest.getCity())
                    .state(addressRequest.getState())
                    .postalCode(addressRequest.getPostalCode())
                    .country(addressRequest.getCountry())
                    .additionalInfo(addressRequest.getAdditionalInfo())
                    .latitude(addressRequest.getLatitude())
                    .longitude(addressRequest.getLongitude())
                    .build();
            newAddress = addressRepository.save(newAddress);
        }

        AssetLocationType newLocationType = AssetLocationType.WORKER_LOCATION;

        // Update the Assignment snapshot
        assignment.setAddress(newAddress);
        assignment.setLocationType(newLocationType);
        assignmentRepository.save(assignment);

        // Update the live Asset record
        Asset asset = assignment.getAsset();
        asset.setAddress(newAddress);
        asset.setLocationType(newLocationType);
        assetRepository.save(asset);

        return mapAssignmentToResponse(assignment);
    }

    // ==========================================
    // MAPPERS
    // ==========================================

    private AssetAssignmentResponse mapAssignmentToResponse(AssetJobAssignment a) {
        long durationDays = a.isActive() ? nullSafeDaysBetween(a.getAssignedAt(), LocalDateTime.now(ZoneOffset.UTC))
                : nullSafeDaysBetween(a.getAssignedAt(), a.getReturnedAt());
        String status = a.isActive() ? "ACTIVE" : "COMPLETED";

        AddressResponse addressResponse = null;
        if (a.getAddress() != null) {
            Address addr = a.getAddress();
            addressResponse = AddressResponse.builder()
                    .id(addr.getId())
                    .street(addr.getStreet())
                    .city(addr.getCity())
                    .state(addr.getState())
                    .postalCode(addr.getPostalCode())
                    .country(addr.getCountry())
                    .additionalInfo(addr.getAdditionalInfo())
                    .latitude(addr.getLatitude())
                    .longitude(addr.getLongitude())
                    .build();
        }

        return AssetAssignmentResponse.builder()
                .assignmentId(a.getId())
                .assetId(a.getAsset().getId())
                .jobId(a.getJob() != null ? a.getJob().getId() : null)
                .assignedWorkerId(a.getAssignedWorker() != null ? a.getAssignedWorker().getId() : null)
                .assetName(a.getAsset().getName())
                .description(a.getAsset().getDescription())
                .serialNumber(a.getAsset().getSerialNumber())
                .assetTag(a.getAsset().getAssetTag())
                .notes(a.getNotes())
                .assignedAt(a.getAssignedAt())
                .returnedAt(a.getReturnedAt())
                .durationDays(durationDays)
                .expectedDurationDays(a.getExpectedDurationDays())
                .slaBreached(a.isSlaBreached())
                .status(status)
                .locationType(a.getLocationType())
                .address(addressResponse)
                .build();
    }

    private AssetResponse mapAssetToResponse(Asset asset) {
        AddressResponse currentAddr = null;
        if (asset.getAddress() != null) {
            currentAddr = AddressResponse.builder()
                    .id(asset.getAddress().getId())
                    .street(asset.getAddress().getStreet())
                    .city(asset.getAddress().getCity())
                    .state(asset.getAddress().getState())
                    .postalCode(asset.getAddress().getPostalCode())
                    .country(asset.getAddress().getCountry())
                    .additionalInfo(asset.getAddress().getAdditionalInfo())
                    .latitude(asset.getAddress().getLatitude())
                    .longitude(asset.getAddress().getLongitude())
                    .build();
        }

        return AssetResponse.builder()
                .id(asset.getId())
                .assetRef(asset.getAssetRef())
                .companyId(asset.getCompany().getId())
                .name(asset.getName())
                .description(asset.getDescription())
                .serialNumber(asset.getSerialNumber())
                .assetTag(asset.getAssetTag())
                // Hide financial data like purchasePrice/depreciationRate from workers if
                // desired,
                // but if using the standard AssetResponse DTO we map everything.
                // (Set to null here if you want to mask financial data from the worker app)
                .available(asset.isAvailable())
                .archived(asset.isArchived())
                .locationType(asset.getLocationType())
                .address(currentAddr)
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

    private long nullSafeDaysBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null)
            return 0L;
        return Duration.between(from, to).toDays();
    }
}