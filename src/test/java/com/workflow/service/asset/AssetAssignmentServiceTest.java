package com.workflow.service.asset;

import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.exception.business.AssetNotFoundException;
import com.workflow.common.exception.business.JobNotFoundException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.dto.asset.AssetAssignmentCreateRequest;
import com.workflow.dto.asset.AssetAssignmentResponse;
import com.workflow.dto.asset.AssetAssignmentReturnRequest;
import com.workflow.entity.*;
import com.workflow.repository.AssetJobAssignmentRepository;
import com.workflow.repository.AssetRepository;
import com.workflow.repository.JobRepository;
import com.workflow.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetAssignmentServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetJobAssignmentRepository assignmentRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private AssetAssignmentService assetAssignmentService;

    private Company company;
    private Asset asset;
    private Job job;
    private Worker worker;
    private AssetJobAssignment assignment;
    private AssetAssignmentCreateRequest createRequest;
    private AssetAssignmentReturnRequest returnRequest;

    @BeforeEach
    void setUp() {
        company = Company.builder()
                .id(1L)
                .name("Test Company")
                .build();

        asset = Asset.builder()
                .id(1L)
                .company(company)
                .name("Excavator XL-500")
                .assetTag("EQ-001")
                .purchasePrice(new BigDecimal("45000.00"))
                .purchaseDate(LocalDate.of(2024, 1, 15))
                .depreciationRate(new BigDecimal("20.00"))
                .salvageValue(new BigDecimal("5000.00"))
                .available(true)
                .archived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        job = Job.builder()
                .id(1L)
                .company(company)
                .status(JobStatus.IN_PROGRESS)
                .build();

        worker = Worker.builder()
                .id(1L)
                .company(company)
                .name("John Doe")
                .email("john.doe@example.com")
                .build();

        assignment = AssetJobAssignment.builder()
                .id(1L)
                .asset(asset)
                .job(job)
                .assignedWorker(worker)
                .notes("Assignment notes")
                .assignedAt(LocalDateTime.now().minusDays(5))
                .build();

        createRequest = AssetAssignmentCreateRequest.builder()
                .assetId(1L)
                .jobId(1L)
                .assignedWorkerId(1L)
                .notes("For foundation work")
                .build();

        returnRequest = AssetAssignmentReturnRequest.builder()
                .assignmentId(1L)
                .notes("Returned in good condition")
                .build();
    }

    // ==================== ASSIGN ASSET TESTS ====================

    @Test
    void assignAsset_Success_WithJobAndWorker() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(assignmentRepository.save(any(AssetJobAssignment.class))).thenAnswer(invocation -> {
            AssetJobAssignment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        AssetAssignmentResponse response = assetAssignmentService.assignAsset(createRequest, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getAssetId()).isEqualTo(1L);
        assertThat(response.getJobId()).isEqualTo(1L);
        assertThat(response.getAssignedWorkerId()).isEqualTo(1L);
        assertThat(response.getNotes()).isEqualTo("For foundation work");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        verify(assignmentRepository).save(any(AssetJobAssignment.class));
        verify(assetRepository).save(asset);
        assertThat(asset.isAvailable()).isFalse();
        assertThat(asset.getCurrentLocation()).isEqualTo("On job: 1");
    }

    @Test
    void assignAsset_Success_WithJobOnly() {
        createRequest.setAssignedWorkerId(null);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(assignmentRepository.save(any(AssetJobAssignment.class))).thenAnswer(invocation -> {
            AssetJobAssignment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        AssetAssignmentResponse response = assetAssignmentService.assignAsset(createRequest, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo(1L);
        assertThat(response.getAssignedWorkerId()).isNull();
        verify(workerRepository, never()).findById(any());
    }

    @Test
    void assignAsset_Success_WithWorkerOnly() {
        createRequest.setJobId(null);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(assignmentRepository.save(any(AssetJobAssignment.class))).thenAnswer(invocation -> {
            AssetJobAssignment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        AssetAssignmentResponse response = assetAssignmentService.assignAsset(createRequest, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isNull();
        assertThat(response.getAssignedWorkerId()).isEqualTo(1L);
        assertThat(asset.getCurrentLocation()).contains("worker:1");
        verify(jobRepository, never()).findById(any());
    }

    @Test
    void assignAsset_AssetNotFound_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetAssignmentService.assignAsset(createRequest, 1L))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found");

        verify(assignmentRepository, never()).save(any());
        verify(assetRepository, never()).save(any());
    }

    @Test
    void assignAsset_WrongCompany_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetAssignmentService.assignAsset(createRequest, 999L))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assignAsset_ArchivedAsset_ThrowsException() {
        asset.setArchived(true);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetAssignmentService.assignAsset(createRequest, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot assign archived asset");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assignAsset_AlreadyAssigned_ThrowsException() {
        AssetJobAssignment existingAssignment = AssetJobAssignment.builder()
                .id(2L)
                .asset(asset)
                .assignedAt(LocalDateTime.now())
                .build();

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.of(existingAssignment));

        assertThatThrownBy(() -> assetAssignmentService.assignAsset(createRequest, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Asset is already assigned");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assignAsset_JobNotFound_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(jobRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetAssignmentService.assignAsset(createRequest, 1L))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessage("Job not found");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assignAsset_JobWrongCompany_ThrowsException() {
        Company otherCompany = Company.builder().id(999L).name("Other Company").build();
        job.setCompany(otherCompany);

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> assetAssignmentService.assignAsset(createRequest, 1L))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessage("Job not found");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assignAsset_CompletedJob_ThrowsException() {
        job.setStatus(JobStatus.COMPLETED);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> assetAssignmentService.assignAsset(createRequest, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot assign asset to a completed or cancelled job");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assignAsset_CancelledJob_ThrowsException() {
        job.setStatus(JobStatus.CANCELLED);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> assetAssignmentService.assignAsset(createRequest, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot assign asset to a completed or cancelled job");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assignAsset_WorkerNotFound_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(workerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetAssignmentService.assignAsset(createRequest, 1L))
                .isInstanceOf(WorkerNotFoundException.class)
                .hasMessage("Worker not found");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assignAsset_WorkerWrongCompany_ThrowsException() {
        Company otherCompany = Company.builder().id(999L).name("Other Company").build();
        worker.setCompany(otherCompany);

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertThatThrownBy(() -> assetAssignmentService.assignAsset(createRequest, 1L))
                .isInstanceOf(WorkerNotFoundException.class)
                .hasMessage("Worker not found");

        verify(assignmentRepository, never()).save(any());
    }

    // ==================== RETURN ASSET TESTS ====================

    @Test
    void returnAsset_Success() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(AssetJobAssignment.class))).thenReturn(assignment);
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        AssetAssignmentResponse response = assetAssignmentService.returnAsset(returnRequest, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(assignment.getReturnedAt()).isNotNull();
        assertThat(assignment.getNotes()).contains("Returned in good condition");

        verify(assignmentRepository).save(assignment);
        verify(assetRepository).save(asset);
        assertThat(asset.isAvailable()).isTrue();
        assertThat(asset.getCurrentLocation()).isEqualTo("Returned");
    }

    @Test
    void returnAsset_Success_WithExistingNotes() {
        assignment.setNotes("Initial notes");
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(AssetJobAssignment.class))).thenReturn(assignment);
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        AssetAssignmentResponse response = assetAssignmentService.returnAsset(returnRequest, 1L);

        assertThat(response).isNotNull();
        assertThat(assignment.getNotes()).contains("Initial notes");
        assertThat(assignment.getNotes()).contains("Returned in good condition");
    }

    @Test
    void returnAsset_Success_AssetStaysUnavailableWithOtherActiveAssignment() {
        AssetJobAssignment otherActiveAssignment = AssetJobAssignment.builder()
                .id(2L)
                .asset(asset)
                .assignedAt(LocalDateTime.now())
                .build();

        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.of(otherActiveAssignment));
        when(assignmentRepository.save(any(AssetJobAssignment.class))).thenReturn(assignment);

        AssetAssignmentResponse response = assetAssignmentService.returnAsset(returnRequest, 1L);

        assertThat(response).isNotNull();
        verify(assetRepository, never()).save(any());
    }

    @Test
    void returnAsset_AssignmentNotFound_ThrowsException() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetAssignmentService.returnAsset(returnRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Assignment not found");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void returnAsset_WrongCompany_ThrowsException() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> assetAssignmentService.returnAsset(returnRequest, 999L))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Assignment not found for company");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void returnAsset_AlreadyReturned_ThrowsException() {
        assignment.setReturnedAt(LocalDateTime.now());
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> assetAssignmentService.returnAsset(returnRequest, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Assignment already returned");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void returnAsset_WithoutReturnNotes_Success() {
        returnRequest.setNotes(null);
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(AssetJobAssignment.class))).thenReturn(assignment);
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        AssetAssignmentResponse response = assetAssignmentService.returnAsset(returnRequest, 1L);

        assertThat(response).isNotNull();
        assertThat(assignment.getNotes()).isEqualTo("Assignment notes");
    }

    // ==================== GET ASSIGNMENT HISTORY TESTS ====================

    @Test
    void getAssignmentHistory_Success() {
        AssetJobAssignment assignment2 = AssetJobAssignment.builder()
                .id(2L)
                .asset(asset)
                .job(job)
                .assignedAt(LocalDateTime.now().minusDays(10))
                .returnedAt(LocalDateTime.now().minusDays(8))
                .build();

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdOrderByAssignedAtDesc(1L))
                .thenReturn(Arrays.asList(assignment, assignment2));

        List<AssetAssignmentResponse> history = assetAssignmentService.getAssignmentHistory(1L, 1L);

        assertThat(history).isNotNull();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getAssignmentId()).isEqualTo(1L);
        assertThat(history.get(1).getAssignmentId()).isEqualTo(2L);
    }

    @Test
    void getAssignmentHistory_AssetNotFound_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetAssignmentService.getAssignmentHistory(1L, 1L))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found");
    }

    @Test
    void getAssignmentHistory_WrongCompany_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetAssignmentService.getAssignmentHistory(1L, 999L))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found");
    }

    @Test
    void getAssignmentHistory_NoHistory_ReturnsEmptyList() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdOrderByAssignedAtDesc(1L)).thenReturn(List.of());

        List<AssetAssignmentResponse> history = assetAssignmentService.getAssignmentHistory(1L, 1L);

        assertThat(history).isNotNull();
        assertThat(history).isEmpty();
    }

    // ==================== GET ASSIGNED ASSETS FOR JOB TESTS ====================

    @Test
    void getAssignedAssetsForJob_OnlyActive_Success() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(assignmentRepository.findByJobIdAndReturnedAtIsNull(1L)).thenReturn(List.of(assignment));

        List<AssetAssignmentResponse> assets = assetAssignmentService.getAssignedAssetsForJob(1L, 1L, true);

        assertThat(assets).isNotNull();
        assertThat(assets).hasSize(1);
        assertThat(assets.get(0).getJobId()).isEqualTo(1L);
        verify(assignmentRepository).findByJobIdAndReturnedAtIsNull(1L);
    }

    @Test
    void getAssignedAssetsForJob_AllAssignments_Success() {
        AssetJobAssignment returnedAssignment = AssetJobAssignment.builder()
                .id(2L)
                .asset(asset)
                .job(job)
                .assignedAt(LocalDateTime.now().minusDays(10))
                .returnedAt(LocalDateTime.now().minusDays(8))
                .build();

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(assignmentRepository.findByJobId(1L)).thenReturn(Arrays.asList(assignment, returnedAssignment));

        List<AssetAssignmentResponse> assets = assetAssignmentService.getAssignedAssetsForJob(1L, 1L, false);

        assertThat(assets).isNotNull();
        assertThat(assets).hasSize(2);
    }

    @Test
    void getAssignedAssetsForJob_JobNotFound_ThrowsException() {
        when(jobRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetAssignmentService.getAssignedAssetsForJob(1L, 1L, true))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessage("Job not found");
    }

    @Test
    void getAssignedAssetsForJob_WrongCompany_ThrowsException() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> assetAssignmentService.getAssignedAssetsForJob(1L, 999L, true))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessage("Job not found");
    }

    @Test
    void getAssignedAssetsForJob_NoAssets_ReturnsEmptyList() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(assignmentRepository.findByJobIdAndReturnedAtIsNull(1L)).thenReturn(List.of());

        List<AssetAssignmentResponse> assets = assetAssignmentService.getAssignedAssetsForJob(1L, 1L, true);

        assertThat(assets).isNotNull();
        assertThat(assets).isEmpty();
    }
}