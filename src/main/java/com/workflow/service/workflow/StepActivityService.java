package com.workflow.service.workflow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.entity.JobWorkflowStep;
import com.workflow.entity.JobWorkflowStepActivity;
import com.workflow.entity.User;
import com.workflow.repository.JobWorkflowStepActivityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StepActivityService implements IStepActivityService {

    private final JobWorkflowStepActivityRepository activityRepository;

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
}
