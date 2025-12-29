package com.workflow.service.workflow;

import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.dto.workflow.*;
import com.workflow.entity.*;
import com.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class JobWorkflowService implements IJobWorkflowService {

        private final JobWorkflowRepository jobWorkflowRepository;
        private final JobWorkflowStepRepository jobWorkflowStepRepository;
        private final WorkflowStepRepository workflowStepRepository;

        public JobWorkflowResponse startWorkflow(Job job, Workflow workflow) {

                JobWorkflow jobWorkflow = jobWorkflowRepository.save(
                                JobWorkflow.builder()
                                                .job(job)
                                                .workflow(workflow)
                                                .build());

                List<WorkflowStep> steps = workflowStepRepository
                                .findByWorkflowIdOrderByOrderIndexAsc(workflow.getId());

                for (int i = 0; i < steps.size(); i++) {
                        jobWorkflowStepRepository.save(
                                        JobWorkflowStep.builder()
                                                        .jobWorkflow(jobWorkflow)
                                                        .step(steps.get(i))
                                                        .status(i == 0 ? WorkflowStepStatus.ONGOING
                                                                        : WorkflowStepStatus.PENDING)
                                                        .startedAt(i == 0 ? LocalDateTime.now() : null)
                                                        .build());
                }

                return buildResponse(jobWorkflow);
        }

        public JobWorkflowResponse getJobWorkflow(Job job) {
                JobWorkflow jobWorkflow = jobWorkflowRepository.findByJobId(job.getId())
                                .orElseThrow(() -> new IllegalStateException("Workflow not started for this job"));

                List<JobWorkflowStepResponse> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByStep_OrderIndexAsc(jobWorkflow.getId())
                                .stream()
                                .<JobWorkflowStepResponse>map(s -> JobWorkflowStepResponse.builder()
                                                .id(s.getId())
                                                .name(s.getStep().getName())
                                                .status(s.getStatus())
                                                .startedAt(s.getStartedAt())
                                                .completedAt(s.getCompletedAt())
                                                .build())
                                .toList();

                return JobWorkflowResponse.builder()
                                .jobId(job.getId())
                                .workflowId(jobWorkflow.getWorkflow().getId())
                                .steps(steps)
                                .build();
        }

        public void completeStep(Long stepId) {
                JobWorkflowStep step = jobWorkflowStepRepository.findById(stepId).orElseThrow();
                step.setStatus(WorkflowStepStatus.DONE);
                step.setCompletedAt(LocalDateTime.now());

                jobWorkflowStepRepository.findByJobWorkflowIdOrderByStep_OrderIndexAsc(
                                step.getJobWorkflow().getId())
                                .stream()
                                .filter(s -> s.getStatus() == WorkflowStepStatus.PENDING)
                                .findFirst()
                                .ifPresent(next -> {
                                        next.setStatus(WorkflowStepStatus.ONGOING);
                                        next.setStartedAt(LocalDateTime.now());
                                });
        }

        private JobWorkflowResponse buildResponse(JobWorkflow jw) {
                List<JobWorkflowStepResponse> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByStep_OrderIndexAsc(jw.getId())
                                .stream()
                                .map(s -> JobWorkflowStepResponse.builder()
                                                .id(s.getId())
                                                .name(s.getStep().getName())
                                                .status(s.getStatus())
                                                .build())
                                .toList();

                return JobWorkflowResponse.builder()
                                .jobId(jw.getJob().getId())
                                .workflowId(jw.getWorkflow().getId())
                                .steps(steps)
                                .build();
        }
}
