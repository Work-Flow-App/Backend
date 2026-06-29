package com.workflow.service.asset;

import com.workflow.common.constant.asset.AssetLocationType;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.dto.asset.*;
import com.workflow.dto.job.AddressRequest;
import com.workflow.dto.job.AddressResponse;
import com.workflow.entity.asset.Asset;
import com.workflow.entity.asset.AssetJobAssignment;
import com.workflow.entity.common.Address;
import com.workflow.entity.job.Job;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.asset.AssetJobAssignmentRepository;
import com.workflow.repository.asset.AssetRepository;
import com.workflow.repository.common.AddressRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.common.exception.business.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssetAssignmentService implements IAssetAssignmentService {

    private final AssetRepository assetRepository;
    private final AssetJobAssignmentRepository assignmentRepository;
    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;
    private final AddressRepository addressRepository;

    @Override
    public AssetAssignmentResponse assignAsset(AssetAssignmentCreateRequest request, Long companyId) {
        log.info("Assigning asset: assetId={}, jobId={}, workerId={}, companyId={}",
                request.getAssetId(), request.getJobId(), request.getAssignedWorkerId(), companyId);

        Asset asset = assetRepository.findById(request.getAssetId())
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));

        if (asset.isArchived()) {
            throw new AssignmentException("Cannot assign archived asset");
        }

        Optional<AssetJobAssignment> existing = assignmentRepository.findByAssetIdAndReturnedAtIsNull(asset.getId());
        if (existing.isPresent()) {
            throw new AssignmentException("Asset is already assigned");
        }

        Job job = null;
        if (request.getJobId() != null) {
            job = jobRepository.findById(request.getJobId())
                    .filter(j -> j.getCompany().getId().equals(companyId))
                    .orElseThrow(() -> new JobNotFoundException("Job not found"));

            if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.CANCELLED) {
                throw new AssignmentException("Cannot assign asset to a completed or cancelled job");
            }
        }

        Worker worker = null;
        if (request.getAssignedWorkerId() != null) {
            worker = workerRepository.findById(request.getAssignedWorkerId())
                    .filter(w -> w.getCompany().getId().equals(companyId))
                    .orElseThrow(() -> new WorkerNotFoundException("Worker not found"));
        }

        // 1. Calculate Location FIRST
        AssetLocationType locationType = request.getExplicitLocationType();

        if (locationType == null) {
            if (request.getCustomAddress() != null) {
                locationType = (worker != null) ? AssetLocationType.WORKER_LOCATION : AssetLocationType.CUSTOM;
            } else if (job != null) {
                locationType = AssetLocationType.JOB_SITE;
            } else if (worker != null) {
                locationType = AssetLocationType.WORKER_LOCATION;
            } else {
                locationType = AssetLocationType.WAREHOUSE;
            }
        }

        Address locationAddress = null;

        switch (locationType) {
            case JOB_SITE:
                if (job != null && job.getAddress() != null) {
                    locationAddress = job.getAddress();
                } else if (request.getCustomAddress() != null) {
                    // Sometimes jobs don't have addresses, but the user provides one manually
                    locationAddress = addressRepository.save(mapToAddress(request.getCustomAddress()));
                } else {
                    // Safe fallback for a Job: assume it left from the warehouse
                    locationAddress = asset.getWarehouseAddress();
                }
                break;

            case WORKER_LOCATION:
            case CUSTOM:
                if (request.getCustomAddress() != null) {
                    locationAddress = addressRepository.save(mapToAddress(request.getCustomAddress()));
                }
                break;

            case WAREHOUSE:
                locationAddress = asset.getWarehouseAddress();
                break;
        }

        // 2. Save Assignment WITH Snapshot
        AssetJobAssignment assignment = AssetJobAssignment.builder()
                .asset(asset)
                .job(job)
                .assignedWorker(worker)
                .notes(request.getNotes())
                .assignedAt(LocalDateTime.now(ZoneOffset.UTC))
                .locationType(locationType) // Saving for history
                .address(locationAddress) // Saving for history
                .expectedDurationDays(request.getExpectedDurationDays()) // <-- Add this
                .slaBreached(false)
                .build();

        assignmentRepository.save(assignment);

        // 3. Update Asset's current live state
        asset.setAvailable(false);
        asset.setLocationType(locationType);
        asset.setAddress(locationAddress);
        assetRepository.save(asset);

        log.info("Asset assigned successfully...");
        return mapAssignmentToResponse(assignment);
    }

    @Override
    public AssetAssignmentResponse returnAsset(AssetAssignmentReturnRequest request, Long companyId) {
        log.info("Returning asset: assignmentId={}, companyId={}", request.getAssignmentId(), companyId);

        AssetJobAssignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new AssetAssignmentNotFoundException("Assignment not found"));

        if (!assignment.getAsset().getCompany().getId().equals(companyId)) {
            throw new AssetNotFoundException("Assignment not found for company");
        }

        if (!assignment.isActive()) {
            throw new IllegalStateException("Assignment already returned");
        }

        assignment.setReturnedAt(LocalDateTime.now(ZoneOffset.UTC));
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            assignment.setNotes(Optional.ofNullable(assignment.getNotes()).map(n -> n + "\n" + request.getNotes())
                    .orElse(request.getNotes()));
        }
        assignmentRepository.save(assignment);

        Optional<AssetJobAssignment> active = assignmentRepository
                .findByAssetIdAndReturnedAtIsNull(assignment.getAsset().getId());

        if (active.isEmpty()) {
            Asset asset = assignment.getAsset();
            asset.setAvailable(true);
            asset.setLocationType(AssetLocationType.WAREHOUSE);
            asset.setAddress(asset.getWarehouseAddress());
            assetRepository.save(asset);
        }

        return mapAssignmentToResponse(assignment);
    }

    @Override
    public AssetAssignmentResponse updateAssignment(Long assignmentId, AssetAssignmentUpdateRequest request,
            Long companyId) {
        AssetJobAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssetAssignmentNotFoundException("Assignment not found"));

        if (!assignment.getAsset().getCompany().getId().equals(companyId)) {
            throw new AssetNotFoundException("Assignment not found for company");
        }

        if (!assignment.isActive()) {
            throw new AssignmentException("Cannot update a completed/returned assignment");
        }

        // 1. Update Worker and Notes
        if (request.getAssignedWorkerId() != null) {
            Worker worker = workerRepository.findById(request.getAssignedWorkerId())
                    .filter(w -> w.getCompany().getId().equals(companyId))
                    .orElseThrow(() -> new WorkerNotFoundException("Worker not found"));
            assignment.setAssignedWorker(worker);
        } else if (request.getAssignedWorkerId() == null && assignment.getJob() == null) {
            assignment.setAssignedWorker(null);
        }

        if (request.getNotes() != null) {
            assignment.setNotes(request.getNotes());
        }

        // Duration update logic
        if (request.getExpectedDurationDays() != null) {
            Integer oldDuration = assignment.getExpectedDurationDays();
            Integer newDuration = request.getExpectedDurationDays();

            assignment.setExpectedDurationDays(newDuration);

            // If it is currently breached, check if the new deadline fixes it
            if (assignment.isSlaBreached()) {
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                long daysElapsed = Duration.between(assignment.getAssignedAt(), now).toDays();

                // If the new deadline is greater than the days already elapsed, reset the
                // breach!
                if (newDuration > daysElapsed) {
                    assignment.setSlaBreached(false);

                    // Log this extension in the notes so you have an audit trail of the override
                    String extensionMessage = String.format(
                            "\n[SYSTEM ALERT - %s] SLA Extended. Deadline increased from %d to %d days. Breach status reset.",
                            now.toLocalDate().toString(),
                            oldDuration != null ? oldDuration : 0,
                            newDuration);

                    String currentNotes = assignment.getNotes() == null ? "" : assignment.getNotes();
                    assignment.setNotes(currentNotes + extensionMessage);
                }
            }
        }

        // 2. Handle Location Updates
        Asset asset = assignment.getAsset();
        AssetLocationType oldType = assignment.getLocationType();

        AssetLocationType newType = request.getExplicitLocationType();
        if (newType == null) {
            newType = oldType;
        }

        Address newAddress = assignment.getAddress();

        if (request.getCustomAddress() != null
                && (newType == AssetLocationType.WORKER_LOCATION || newType == AssetLocationType.CUSTOM)) {
            newAddress = addressRepository.save(mapToAddress(request.getCustomAddress()));
        } else if (newType == AssetLocationType.WAREHOUSE) {
            newAddress = asset.getWarehouseAddress();
        } else if (newType == AssetLocationType.JOB_SITE && assignment.getJob() != null) {
            newAddress = assignment.getJob().getAddress();
        }

        // Update BOTH the assignment snapshot and the live asset
        assignment.setLocationType(newType);
        assignment.setAddress(newAddress);
        assignmentRepository.save(assignment);

        asset.setLocationType(newType);
        asset.setAddress(newAddress);
        assetRepository.save(asset);

        return mapAssignmentToResponse(assignment);
    }

    @Override
    public void syncJobAssets(Long jobId, List<Long> assetIds, Long companyId) {
        Job job = jobRepository.findById(jobId)
                .filter(j -> j.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new JobNotFoundException("Job not found"));

        // 1. Get current assignments
        List<AssetJobAssignment> activeAssignments = assignmentRepository
                .findByJobIdAndReturnedAtIsNull(job.getId());

        Set<Long> requestedAssetIds = assetIds != null ? new HashSet<>(assetIds) : new HashSet<>();
        Set<Long> currentAssetIds = activeAssignments.stream()
                .map(a -> a.getAsset().getId())
                .collect(Collectors.toSet());

        // ==========================================
        // 2. PROCESS RETURNS (Unselected Assets)
        // ==========================================
        List<AssetJobAssignment> assignmentsToReturn = activeAssignments.stream()
                .filter(a -> !requestedAssetIds.contains(a.getAsset().getId()))
                .toList();

        if (!assignmentsToReturn.isEmpty()) {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            List<Long> returningAssetIds = assignmentsToReturn.stream()
                    .map(a -> a.getAsset().getId())
                    .toList();

            Map<Long, Long> otherActiveAssignmentByAssetId = assignmentRepository
                    .findByAssetIdInAndReturnedAtIsNull(returningAssetIds)
                    .stream()
                    .filter(a -> !a.getJob().getId().equals(job.getId()))
                    .collect(Collectors.toMap(
                            a -> a.getAsset().getId(),
                            AssetJobAssignment::getId,
                            (a, b) -> a));

            for (AssetJobAssignment assignment : assignmentsToReturn) {
                assignment.setReturnedAt(now);
            }
            assignmentRepository.saveAll(assignmentsToReturn);

            List<Asset> assetsToUpdate = new ArrayList<>();
            for (AssetJobAssignment assignment : assignmentsToReturn) {
                Asset asset = assignment.getAsset();
                if (!otherActiveAssignmentByAssetId.containsKey(asset.getId())) {
                    asset.setAvailable(true);
                    asset.setLocationType(AssetLocationType.WAREHOUSE);
                    asset.setAddress(asset.getWarehouseAddress());
                    assetsToUpdate.add(asset);
                }
            }
            if (!assetsToUpdate.isEmpty()) {
                assetRepository.saveAll(assetsToUpdate);
            }
        }

        // ==========================================
        // 3. PROCESS NEW ASSIGNMENTS (Newly Selected)
        // ==========================================
        List<Long> assetsToAdd = requestedAssetIds.stream()
                .filter(id -> !currentAssetIds.contains(id))
                .toList();

        if (!assetsToAdd.isEmpty()) {
            Map<Long, Asset> assetMap = assetRepository.findAllById(assetsToAdd)
                    .stream()
                    .collect(Collectors.toMap(Asset::getId, a -> a));

            Map<Long, AssetJobAssignment> activeAssignmentByAssetId = assignmentRepository
                    .findByAssetIdInAndReturnedAtIsNull(assetsToAdd)
                    .stream()
                    .collect(Collectors.toMap(
                            a -> a.getAsset().getId(),
                            a -> a,
                            (a, b) -> a));

            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            List<AssetJobAssignment> newAssignments = new ArrayList<>();

            for (Long assetId : assetsToAdd) {
                Asset asset = assetMap.get(assetId);
                if (asset == null || !asset.getCompany().getId().equals(companyId)) {
                    throw new AssetNotFoundException("Asset not found with id: " + assetId);
                }
                if (asset.isArchived()) {
                    throw new AssignmentException("Cannot assign archived asset: " + assetId);
                }
                if (!asset.isAvailable()) {
                    AssetJobAssignment activeAssignment = activeAssignmentByAssetId.get(assetId);
                    if (activeAssignment != null) {
                        throw new AssignmentException("Asset " + assetId + " is already assigned to job "
                                + activeAssignment.getJob().getId());
                    }
                    throw new AssignmentException("Asset is not available: " + assetId);
                }

                AssetLocationType locType = AssetLocationType.JOB_SITE;
                Address locAddress = job.getAddress() != null ? job.getAddress() : asset.getWarehouseAddress();

                newAssignments.add(AssetJobAssignment.builder()
                        .asset(asset)
                        .job(job)
                        .assignedAt(now)
                        .locationType(locType)
                        .address(locAddress)
                        .build());

                asset.setAvailable(false);
                asset.setLocationType(locType);
                asset.setAddress(locAddress);
            }

            assignmentRepository.saveAll(newAssignments);
            assetRepository.saveAll(new ArrayList<>(assetMap.values()));
        }
    }

    @Override
    public List<AssetAssignmentResponse> getAssignmentHistory(Long assetId, Long companyId) {
        Asset asset = assetRepository.findById(assetId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));

        List<AssetJobAssignment> list = assignmentRepository.findByAssetIdOrderByAssignedAtDesc(asset.getId());
        return list.stream().map(this::mapAssignmentToResponse).collect(Collectors.toList());
    }

    @Override
    public List<AssetAssignmentResponse> getAssignedAssetsForJob(Long jobId, Long companyId, boolean onlyActive) {
        Job job = jobRepository.findById(jobId)
                .filter(j -> j.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new JobNotFoundException("Job not found"));

        List<AssetJobAssignment> list = onlyActive
                ? assignmentRepository.findByJobIdAndReturnedAtIsNull(job.getId())
                : assignmentRepository.findByJobId(job.getId());

        return list.stream().map(this::mapAssignmentToResponse).collect(Collectors.toList());
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

    private AssetAssignmentResponse mapAssignmentToResponse(AssetJobAssignment a) {
        long durationDays = a.isActive() ? nullSafeDaysBetween(a.getAssignedAt(), LocalDateTime.now(ZoneOffset.UTC))
                : nullSafeDaysBetween(a.getAssignedAt(), a.getReturnedAt());
        String status = a.isActive() ? "ACTIVE" : "COMPLETED";

        AddressResponse addressResponse = null;

        // PULL FROM ASSIGNMENT SNAPSHOT
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
                .locationType(a.getLocationType()) // PULL FROM ASSIGNMENT
                .address(addressResponse) // PULL FROM ASSIGNMENT
                .build();
    }

    private long nullSafeDaysBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null)
            return 0L;
        return Duration.between(from, to).toDays();
    }
}