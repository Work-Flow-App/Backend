package com.workflow.service.asset;

import com.workflow.dto.asset.*;
import com.workflow.entity.*;
import com.workflow.repository.*;
import com.workflow.common.exception.customException.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AssetAssignmentService implements IAssetAssignmentService {

    private final AssetRepository assetRepository;
    private final AssetJobAssignmentRepository assignmentRepository;
    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;

    @Override
    public AssetAssignmentResponse assignAsset(AssetAssignmentCreateRequest request, Long companyId) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new AssetNotFoundException("Asset not found"));

        if (asset.isArchived())
            throw new IllegalStateException("Cannot assign archived asset");

        // asset must be available
        Optional<AssetJobAssignment> existing = assignmentRepository.findByAssetIdAndReturnedAtIsNull(asset.getId());
        if (existing.isPresent())
            throw new IllegalStateException("Asset is already assigned");

        Job job = null;
        if (request.getJobId() != null) {
            job = jobRepository.findById(request.getJobId())
                    .filter(j -> j.getCompany().getId().equals(companyId))
                    .orElseThrow(() -> new JobNotFoundException("Job not found"));
            // job cannot be COMPLETED or CANCELLED
            if ("COMPLETED".equalsIgnoreCase(job.getStatus()) || "CANCELLED".equalsIgnoreCase(job.getStatus())) {
                throw new IllegalStateException("Cannot assign asset to a completed or cancelled job");
            }
        }

        Worker worker = null;
        if (request.getAssignedWorkerId() != null) {
            worker = workerRepository.findById(request.getAssignedWorkerId())
                    .filter(w -> w.getCompany().getId().equals(companyId))
                    .orElseThrow(() -> new WorkerNotFoundException("Worker not found"));
        }

        AssetJobAssignment assignment = AssetJobAssignment.builder()
                .asset(asset)
                .job(job)
                .assignedWorker(worker)
                .notes(request.getNotes())
                .assignedAt(LocalDateTime.now())
                .build();

        assignmentRepository.save(assignment);

        // update asset availability and possibly location (if assigned to job, could
        // update location)
        asset.setAvailable(false);
        if (request.getJobId() != null && job != null) {
            asset.setCurrentLocation("On job: " + job.getId()); // or more detailed job title if needed
        } else if (worker != null) {
            asset.setCurrentLocation(
                    "With " + "worker:" + worker.getId());
        }
        assetRepository.save(asset);

        return mapAssignmentToResponse(assignment);
    }

    @Override
    public AssetAssignmentResponse returnAsset(AssetAssignmentReturnRequest request, Long companyId) {
        AssetJobAssignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        // check asset company
        if (!assignment.getAsset().getCompany().getId().equals(companyId)) {
            throw new AssetNotFoundException("Assignment not found for company");
        }

        if (assignment.getReturnedAt() != null) {
            throw new IllegalStateException("Assignment already returned");
        }

        assignment.setReturnedAt(LocalDateTime.now());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            assignment.setNotes(Optional.ofNullable(assignment.getNotes()).map(n -> n + "\n" + request.getNotes())
                    .orElse(request.getNotes()));
        }
        assignmentRepository.save(assignment);

        // set asset available = true (only if no other active assignment)
        Optional<AssetJobAssignment> active = assignmentRepository
                .findByAssetIdAndReturnedAtIsNull(assignment.getAsset().getId());
        if (active.isEmpty()) {
            Asset asset = assignment.getAsset();
            asset.setAvailable(true);
            // optionally set location to warehouse/office or keep as provided in notes —
            // for now set to "Returned"
            asset.setCurrentLocation("Returned");
            assetRepository.save(asset);
        }

        return mapAssignmentToResponse(assignment);
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
        // verify job belongs to company
        Job job = jobRepository.findById(jobId)
                .filter(j -> j.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new JobNotFoundException("Job not found"));

        List<AssetJobAssignment> list;
        if (onlyActive) {
            list = assignmentRepository.findByJobIdAndReturnedAtIsNull(job.getId());
        } else {
            // get all by job id - we'd need a repository method; for simplicity fetch all
            // assignments and filter
            list = assignmentRepository.findAll().stream()
                    .filter(a -> a.getJob() != null && jobId.equals(a.getJob().getId()))
                    .collect(Collectors.toList());
        }
        return list.stream().map(this::mapAssignmentToResponse).collect(Collectors.toList());
    }

    private AssetAssignmentResponse mapAssignmentToResponse(AssetJobAssignment a) {
        long durationDays = a.getReturnedAt() == null ? nullSafeDaysBetween(a.getAssignedAt(), LocalDateTime.now())
                : nullSafeDaysBetween(a.getAssignedAt(), a.getReturnedAt());
        String status = a.getReturnedAt() == null ? "ACTIVE" : "COMPLETED";
        return AssetAssignmentResponse.builder()
                .assignmentId(a.getId())
                .assetId(a.getAsset().getId())
                .jobId(a.getJob() != null ? a.getJob().getId() : null)
                .assignedWorkerId(a.getAssignedWorker() != null ? a.getAssignedWorker().getId() : null)
                .notes(a.getNotes())
                .assignedAt(a.getAssignedAt())
                .returnedAt(a.getReturnedAt())
                .durationDays(durationDays)
                .status(status)
                .build();
    }

    private long nullSafeDaysBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null)
            return 0L;
        return Duration.between(from, to).toDays();
    }
}
