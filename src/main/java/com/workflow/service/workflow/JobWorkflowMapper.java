package com.workflow.service.workflow;

import com.workflow.common.constant.workflow.SlaStatus;
import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.entity.job.JobWorkflow;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.job.JobWorkflowStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
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
                                .expectedDurationMinutes(step.getExpectedDurationMinutes())
                                .maximumDurationMinutes(step.getMaximumDurationMinutes())
                                .slaStatus(calculateSlaStatus(step))
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

        private SlaStatus calculateSlaStatus(JobWorkflowStep step) {
                if (step.getStartedAt() == null ||
                                (step.getExpectedDurationMinutes() == null
                                                && step.getMaximumDurationMinutes() == null)) {
                        return SlaStatus.NOT_APPLICABLE;
                }

                LocalDateTime endTime = step.getCompletedAt() != null ? step.getCompletedAt() : LocalDateTime.now();
                long minutesElapsed = Duration.between(step.getStartedAt(), endTime).toMinutes();

                if (step.getMaximumDurationMinutes() != null && minutesElapsed > step.getMaximumDurationMinutes()) {
                        return SlaStatus.BREACHED;
                }
                if (step.getExpectedDurationMinutes() != null && minutesElapsed > step.getExpectedDurationMinutes()) {
                        return SlaStatus.ATTENTION_NEEDED;
                }
                return SlaStatus.ON_TRACK;
        }
}
