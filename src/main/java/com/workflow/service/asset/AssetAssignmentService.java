package com.workflow.service.asset;

import com.workflow.common.constant.job.JobStatus;
import com.workflow.dto.asset.*;
import com.workflow.entity.*;
import com.workflow.repository.*;
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

    @Override
    public AssetAssignmentResponse assignAsset(AssetAssignmentCreateRequest request, Long companyId) {
        log.info("Assigning asset: assetId={}, jobId={}, workerId={}, companyId={}",
                 request.getAssetId(), request.getJobId(), request.getAssignedWorkerId(), companyId);

        Asset asset = assetRepository.findById(request.getAssetId())
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> {
                    log.error("Asset not found or does not belong to company: assetId={}, companyId={}",
                              request.getAssetId(), companyId);
                    return new AssetNotFoundException("Asset not found");
                });

        if (asset.isArchived()) {
            log.warn("Attempted to assign archived asset: assetId={}, assetTag={}, companyId={}",
                     asset.getId(), asset.getAssetTag(), companyId);
            throw new IllegalStateException("Cannot assign archived asset");
        }

        // asset must be available
        Optional<AssetJobAssignment> existing = assignmentRepository.findByAssetIdAndReturnedAtIsNull(asset.getId());
        if (existing.isPresent()) {
            log.warn("Attempted to assign already assigned asset: assetId={}, assetTag={}, existingAssignmentId={}",
                     asset.getId(), asset.getAssetTag(), existing.get().getId());
            throw new IllegalStateException("Asset is already assigned");
        }

        Job job = null;
        if (request.getJobId() != null) {
            job = jobRepository.findById(request.getJobId())
                    .filter(j -> j.getCompany().getId().equals(companyId))
                    .orElseThrow(() -> {
                        log.error("Job not found or does not belong to company: jobId={}, companyId={}",
                                  request.getJobId(), companyId);
                        return new JobNotFoundException("Job not found");
                    });
            // job cannot be COMPLETED or CANCELLED
            if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.CANCELLED) {
                log.warn("Attempted to assign asset to completed/cancelled job: assetId={}, jobId={}, jobStatus={}",
                         asset.getId(), job.getId(), job.getStatus());
                throw new IllegalStateException("Cannot assign asset to a completed or cancelled job");
            }
        }

        Worker worker = null;
        if (request.getAssignedWorkerId() != null) {
            worker = workerRepository.findById(request.getAssignedWorkerId())
                    .filter(w -> w.getCompany().getId().equals(companyId))
                    .orElseThrow(() -> {
                        log.error("Worker not found or does not belong to company: workerId={}, companyId={}",
                                  request.getAssignedWorkerId(), companyId);
                        return new WorkerNotFoundException("Worker not found");
                    });
        }

        AssetJobAssignment assignment = AssetJobAssignment.builder()
                .asset(asset)
                .job(job)
                .assignedWorker(worker)
                .notes(request.getNotes())
                .assignedAt(LocalDateTime.now())
                .build();

        assignmentRepository.save(assignment);

        // update asset availability
        asset.setAvailable(false);
        assetRepository.save(asset);

        log.info("Asset assigned successfully: assignmentId={}, assetId={}, assetTag={}, jobId={}, workerId={}",
                 assignment.getId(), asset.getId(), asset.getAssetTag(),
                 job != null ? job.getId() : null, worker != null ? worker.getId() : null);

        return mapAssignmentToResponse(assignment);
    }

    @Override
    public AssetAssignmentResponse returnAsset(AssetAssignmentReturnRequest request, Long companyId) {
        log.info("Returning asset: assignmentId={}, companyId={}", request.getAssignmentId(), companyId);

        AssetJobAssignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> {
                    log.error("Assignment not found: assignmentId={}", request.getAssignmentId());
                    return new AssetAssignmentNotFoundException("Assignment not found");
                });

        // check asset company
        if (!assignment.getAsset().getCompany().getId().equals(companyId)) {
            log.error("Assignment does not belong to company: assignmentId={}, assignmentCompanyId={}, requestCompanyId={}",
                      assignment.getId(), assignment.getAsset().getCompany().getId(), companyId);
            throw new AssetNotFoundException("Assignment not found for company");
        }

        if (!assignment.isActive()) {
            log.warn("Attempted to return already returned assignment: assignmentId={}, assetId={}, returnedAt={}",
                     assignment.getId(), assignment.getAsset().getId(), assignment.getReturnedAt());
            throw new IllegalStateException("Assignment already returned");
        }

        Long assetId = assignment.getAsset().getId();
        String assetTag = assignment.getAsset().getAssetTag();
        long durationDays = Duration.between(assignment.getAssignedAt(), LocalDateTime.now()).toDays();

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
            assetRepository.save(asset);
            log.info("Asset returned and marked as available: assignmentId={}, assetId={}, assetTag={}, durationDays={}",
                     assignment.getId(), assetId, assetTag, durationDays);
        } else {
            log.info("Asset returned but remains unavailable (other active assignment): assignmentId={}, assetId={}, assetTag={}, durationDays={}, otherActiveAssignmentId={}",
                     assignment.getId(), assetId, assetTag, durationDays, active.get().getId());
        }

        return mapAssignmentToResponse(assignment);
    }

    @Override
    public List<AssetAssignmentResponse> getAssignmentHistory(Long assetId, Long companyId) {
        log.debug("Fetching assignment history: assetId={}, companyId={}", assetId, companyId);

        Asset asset = assetRepository.findById(assetId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> {
                    log.error("Asset not found when fetching history: assetId={}, companyId={}", assetId, companyId);
                    return new AssetNotFoundException("Asset not found");
                });

        List<AssetJobAssignment> list = assignmentRepository.findByAssetIdOrderByAssignedAtDesc(asset.getId());
        log.debug("Retrieved assignment history: assetId={}, assetTag={}, historyCount={}",
                  assetId, asset.getAssetTag(), list.size());

        return list.stream().map(this::mapAssignmentToResponse).collect(Collectors.toList());
    }

    @Override
    public List<AssetAssignmentResponse> getAssignedAssetsForJob(Long jobId, Long companyId, boolean onlyActive) {
        log.debug("Fetching assigned assets for job: jobId={}, companyId={}, onlyActive={}",
                  jobId, companyId, onlyActive);

        // verify job belongs to company
        Job job = jobRepository.findById(jobId)
                .filter(j -> j.getCompany().getId().equals(companyId))
                .orElseThrow(() -> {
                    log.error("Job not found when fetching assets: jobId={}, companyId={}", jobId, companyId);
                    return new JobNotFoundException("Job not found");
                });

        List<AssetJobAssignment> list = onlyActive
            ? assignmentRepository.findByJobIdAndReturnedAtIsNull(job.getId())
            : assignmentRepository.findByJobId(job.getId());

        log.debug("Retrieved assigned assets for job: jobId={}, assignmentCount={}, onlyActive={}",
                  jobId, list.size(), onlyActive);

        return list.stream().map(this::mapAssignmentToResponse).collect(Collectors.toList());
    }

    private AssetAssignmentResponse mapAssignmentToResponse(AssetJobAssignment a) {
        long durationDays = a.isActive() ? nullSafeDaysBetween(a.getAssignedAt(), LocalDateTime.now())
                : nullSafeDaysBetween(a.getAssignedAt(), a.getReturnedAt());
        String status = a.isActive() ? "ACTIVE" : "COMPLETED";
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
