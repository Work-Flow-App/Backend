package com.workflow.service.workflow;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.common.exception.business.*;
import com.workflow.dto.workflow.StepVisitLogCreateRequest;
import com.workflow.dto.workflow.StepVisitLogResponse;
import com.workflow.dto.workflow.StepVisitLogSummaryResponse;
import com.workflow.entity.Company;
import com.workflow.entity.JobWorkflowStep;
import com.workflow.entity.JobWorkflowStepVisitLog;
import com.workflow.entity.User;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.JobWorkflowStepRepository;
import com.workflow.repository.JobWorkflowStepVisitLogRepository;
import com.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class JobWorkflowStepVisitLogService implements IJobWorkflowStepVisitLogService {

    private final JobWorkflowStepVisitLogRepository visitLogRepository;
    private final JobWorkflowStepRepository stepRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final IStepActivityService stepActivityService;

    private Company getCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));
    }

    private JobWorkflowStep getStep(Long stepId, Long companyId) {
        JobWorkflowStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new JobWorkflowStepNotFoundException("Step not found"));

        if (!step.getJobWorkflow().getJob().getCompany().getId().equals(companyId)) {
            throw new UnauthorizedWorkflowAccessException("Unauthorized access to this step");
        }
        return step;
    }

    private void validateTimeLog(LocalTime timeIn, LocalTime timeOut) {
        if (timeIn != null && timeOut != null && timeOut.isBefore(timeIn)) {
            throw new InvalidTimeLogException("End time cannot be before start time.");
        }
    }

    private Long calculateWorkedMinutes(LocalTime timeIn, LocalTime timeOut) {
        if (timeIn == null || timeOut == null)
            return 0L;
        return Duration.between(timeIn, timeOut).toMinutes();
    }

    @Override
    public StepVisitLogResponse addVisitLog(Long stepId, StepVisitLogCreateRequest request, Long companyId) {
        validateTimeLog(request.getTimeIn(), request.getTimeOut());
        Company company = getCompany(companyId);
        JobWorkflowStep step = getStep(stepId, companyId);

        // Determine who the log belongs to (defaults to the company admin creating it)
        User loggedByUser = company.getUser();
        if (request.getLoggedById() != null) {
            loggedByUser = userRepository.findById(request.getLoggedById())
                    .orElseThrow(() -> new UserNotFoundException("User not found for the provided loggedById"));
        }

        JobWorkflowStepVisitLog visitLog = visitLogRepository.save(
                JobWorkflowStepVisitLog.builder()
                        .step(step)
                        .loggedBy(loggedByUser)
                        .visitDate(request.getVisitDate())
                        .timeIn(request.getTimeIn())
                        .timeOut(request.getTimeOut())
                        .description(request.getDescription())
                        .build());

        stepActivityService.log(step, company.getUser(), JobWorkflowStepActivityType.VISIT_LOGGED,
                "Logged a visit for " + request.getVisitDate());

        return map(visitLog);
    }

    @Override
    public StepVisitLogResponse updateVisitLog(Long visitLogId, StepVisitLogCreateRequest request, Long companyId) {
        // Fetch visit log first — throws 404 immediately if it doesn't exist,
        // before making the extra getCompany() DB call.
        JobWorkflowStepVisitLog visitLog = visitLogRepository.findById(visitLogId)
                .orElseThrow(() -> new VisitLogNotFoundException("Visit log not found"));

        // Validate company ownership before resolving the company entity.
        if (!visitLog.getStep().getJobWorkflow().getJob().getCompany().getId().equals(companyId)) {
            throw new ForbiddenActionException("Not allowed to edit visit logs for other companies");
        }

        Company company = getCompany(companyId);

        // Apply fields safely and validate time
        LocalTime newTimeIn = request.getTimeIn() != null ? request.getTimeIn() : visitLog.getTimeIn();
        LocalTime newTimeOut = request.getTimeOut() != null ? request.getTimeOut() : visitLog.getTimeOut();
        validateTimeLog(newTimeIn, newTimeOut);

        if (request.getVisitDate() != null)
            visitLog.setVisitDate(request.getVisitDate());
        if (request.getTimeIn() != null)
            visitLog.setTimeIn(request.getTimeIn());
        if (request.getTimeOut() != null)
            visitLog.setTimeOut(request.getTimeOut());
        if (request.getDescription() != null)
            visitLog.setDescription(request.getDescription());

        // Allow company to update who logged the visit
        if (request.getLoggedById() != null && !visitLog.getLoggedBy().getId().equals(request.getLoggedById())) {
            User newUser = userRepository.findById(request.getLoggedById())
                    .orElseThrow(() -> new UserNotFoundException("User not found for the provided loggedById"));
            visitLog.setLoggedBy(newUser);
        }

        stepActivityService.log(visitLog.getStep(), company.getUser(), JobWorkflowStepActivityType.VISIT_UPDATED,
                "Updated a visit log for " + visitLog.getVisitDate());

        return map(visitLog);
    }

    @Override
    public void deleteVisitLog(Long visitLogId, Long companyId) {
        Company company = getCompany(companyId);
        JobWorkflowStepVisitLog visitLog = visitLogRepository.findById(visitLogId)
                .orElseThrow(() -> new VisitLogNotFoundException("Visit log not found"));

        if (!visitLog.getStep().getJobWorkflow().getJob().getCompany().getId().equals(companyId)) {
            throw new ForbiddenActionException("Not allowed to delete visit logs for other companies");
        }

        visitLogRepository.delete(visitLog);
        stepActivityService.log(visitLog.getStep(), company.getUser(), JobWorkflowStepActivityType.VISIT_DELETED,
                "Deleted a visit log");
    }

    @Override
    @Transactional(readOnly = true)
    public StepVisitLogSummaryResponse getVisitLogs(Long stepId, Long companyId) {
        getStep(stepId, companyId); // Validates access

        List<StepVisitLogResponse> logs = visitLogRepository.findByStepIdOrderByVisitDateDescTimeInDesc(stepId)
                .stream()
                .map(this::map)
                .toList();

        Long totalMinutes = logs.stream()
                .mapToLong(StepVisitLogResponse::getWorkedMinutes)
                .sum();

        return StepVisitLogSummaryResponse.builder()
                .visitLogs(logs)
                .totalWorkedMinutes(totalMinutes)
                .build();
    }

    private StepVisitLogResponse map(JobWorkflowStepVisitLog log) {
        return StepVisitLogResponse.builder()
                .id(log.getId())
                .visitDate(log.getVisitDate())
                .timeIn(log.getTimeIn())
                .timeOut(log.getTimeOut())
                .workedMinutes(calculateWorkedMinutes(log.getTimeIn(), log.getTimeOut()))
                .description(log.getDescription())
                .loggedById(log.getLoggedBy().getId())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}