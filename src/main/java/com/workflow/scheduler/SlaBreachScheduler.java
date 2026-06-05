package com.workflow.scheduler;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.repository.job.JobWorkflowStepRepository;
import com.workflow.service.workflow.IStepActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaBreachScheduler {

    private final JobWorkflowStepRepository stepRepository;
    private final IStepActivityService stepActivityService;

    // Runs every 30 minutes.
    @Scheduled(fixedDelay = 1800000)
    @Transactional
    public void checkSlaBreaches() {
        log.info("Running SLA Breach Checker...");

        // Only pulls steps that are running and haven't been flagged as breached yet
        List<JobWorkflowStep> activeSteps = stepRepository.findActiveStepsWithUnnotifiedSlaBreach();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<JobWorkflowStep> newlyBreachedSteps = new ArrayList<>();

        for (JobWorkflowStep step : activeSteps) {
            long minutesElapsed = Duration.between(step.getStartedAt(), now).toMinutes();

            if (minutesElapsed > step.getMaximumDurationMinutes()) {
                // 1. Flip the circuit breaker so it never gets picked up by this query again
                step.setSlaBreached(true);
                newlyBreachedSteps.add(step);

                // 2. Log it to the timeline
                String breachMessage = String.format(
                        "SLA Breached! Step has been running for %d minutes (Limit: %d minutes)",
                        minutesElapsed, step.getMaximumDurationMinutes());

                stepActivityService.log(
                        step,
                        null, // System actor
                        JobWorkflowStepActivityType.SLA_BREACHED,
                        breachMessage);

                log.warn("SLA Breach logged for Step ID: {}", step.getId());
            }
        }

        // 3. Batch save all the updated flags to the database
        if (!newlyBreachedSteps.isEmpty()) {
            stepRepository.saveAll(newlyBreachedSteps);
        }
    }
}