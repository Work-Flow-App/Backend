package com.workflow.service.workflow;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.common.exception.business.JobWorkflowStepNotFoundException;
import com.workflow.common.exception.business.UnauthorizedWorkflowAccessException;
import com.workflow.dto.workflow.StepActivityResponse;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.job.JobWorkflowStepActivity;
import com.workflow.entity.auth.User;
import com.workflow.repository.job.JobWorkflowStepActivityRepository;
import com.workflow.repository.job.JobWorkflowStepRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StepActivityService implements IStepActivityService {

    private final JobWorkflowStepActivityRepository activityRepository;
    private final JobWorkflowStepRepository stepRepository;

    private JobWorkflowStep getStep(Long stepId, Long companyId) {
        JobWorkflowStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new JobWorkflowStepNotFoundException("Step not found"));

        if (!step.getJobWorkflow()
                .getJob()
                .getCompany()
                .getId()
                .equals(companyId)) {
            throw new UnauthorizedWorkflowAccessException("Unauthorized access");
        }
        return step;
    }

    @Override
    public void log(
            JobWorkflowStep step,
            User actor,
            JobWorkflowStepActivityType type,
            String message) {

        activityRepository.save(
                JobWorkflowStepActivity.builder()
                        .step(step)
                        .actor(actor)
                        .type(type)
                        .message(message)
                        .build());
    }

    @Override
    public void logAll(
            List<JobWorkflowStep> steps,
            User actor,
            JobWorkflowStepActivityType type,
            String message) {

        List<JobWorkflowStepActivity> entries = steps.stream()
                .map(step -> JobWorkflowStepActivity.builder()
                        .step(step)
                        .actor(actor)
                        .type(type)
                        .message(message)
                        .build())
                .toList();

        activityRepository.saveAll(entries);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StepActivityResponse> getTimeline(Long stepId, Long companyId) {

        getStep(stepId, companyId);

        return activityRepository.findByStepIdOrderByCreatedAtAsc(stepId)
                .stream()
                .map(this::map)
                .toList();
    }

    private StepActivityResponse map(JobWorkflowStepActivity a) {
        return StepActivityResponse.builder()
                .id(a.getId())
                .type(a.getType().name())
                .message(a.getMessage())
                .actorId(a.getActor().getId())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
