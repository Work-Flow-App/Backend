package com.workflow.service.workflow;

import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.entity.job.JobWorkflow;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.job.JobWorkflowStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JobWorkflowMapper {

    private final JobWorkflowStepRepository stepRepository;

    /**
     * Maps a single JobWorkflowStep entity to its response DTO.
     * Exposes order_index as 1-based (matching the stored value).
     */
    public JobWorkflowStepResponse mapStep(JobWorkflowStep step) {
        return JobWorkflowStepResponse.builder()
                .id(step.getId())
                .name(step.getName())
                .description(step.getDescription())
                .orderIndex(step.getOrderIndex())
                .status(step.getStatus())
                .startedAt(step.getStartedAt())
                .completedAt(step.getCompletedAt())
                .assignedWorkerIds(
                        step.getAssignedWorkers()
                                .stream()
                                .map(Worker::getId)
                                .collect(Collectors.toSet()))
                .build();
    }

    /**
     * Builds a JobWorkflowResponse from a workflow and its already-loaded steps.
     * Prefer this overload in batch/read paths where steps are fetched externally
     * to avoid an extra query.
     */
    public JobWorkflowResponse buildWorkflowResponse(JobWorkflow jw, List<JobWorkflowStep> steps) {
        List<JobWorkflowStepResponse> stepResponses = steps.stream()
                .map(this::mapStep)
                .toList();

        return JobWorkflowResponse.builder()
                .id(jw.getId())
                .jobId(jw.getJob().getId())
                .steps(stepResponses)
                .status(jw.getStatus())
                .build();
    }

    /**
     * Convenience overload — fetches steps from the DB then delegates to
     * {@link #buildWorkflowResponse(JobWorkflow, List)}.
     * Use only in single-workflow read paths; avoid in loops.
     */
    public JobWorkflowResponse buildWorkflowResponse(JobWorkflow jw) {
        List<JobWorkflowStep> steps = stepRepository
                .findByJobWorkflowIdOrderByOrderIndexAsc(jw.getId());
        return buildWorkflowResponse(jw, steps);
    }
}
