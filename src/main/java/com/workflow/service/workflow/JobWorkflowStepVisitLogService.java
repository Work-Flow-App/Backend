package com.workflow.service.workflow;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.JobWorkflowStepNotFoundException;
import com.workflow.common.exception.business.UnauthorizedWorkflowAccessException;
import com.workflow.common.exception.business.VisitLogNotFoundException;
import com.workflow.dto.workflow.StepVisitLogCreateRequest;
import com.workflow.dto.workflow.StepVisitLogResponse;
import com.workflow.entity.Company;
import com.workflow.entity.JobWorkflowStep;
import com.workflow.entity.JobWorkflowStepVisitLog;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.JobWorkflowStepRepository;
import com.workflow.repository.JobWorkflowStepVisitLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class JobWorkflowStepVisitLogService implements IJobWorkflowStepVisitLogService {

    private final JobWorkflowStepVisitLogRepository visitLogRepository;
    private final JobWorkflowStepRepository stepRepository;
    private final CompanyRepository companyRepository;
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

    @Override
    public StepVisitLogResponse addVisitLog(Long stepId, StepVisitLogCreateRequest request, Long companyId) {
        Company company = getCompany(companyId);
        JobWorkflowStep step = getStep(stepId, companyId);

        JobWorkflowStepVisitLog visitLog = visitLogRepository.save(
                JobWorkflowStepVisitLog.builder()
                        .step(step)
                        .loggedBy(company.getUser())
                        .visitDate(request.getVisitDate())
                        .timeIn(request.getTimeIn())
                        .timeOut(request.getTimeOut())
                        .description(request.getDescription())
                        .build());

        stepActivityService.log(
                step,
                company.getUser(),
                JobWorkflowStepActivityType.VISIT_LOGGED,
                "Logged a visit for " + request.getVisitDate());

        return map(visitLog);
    }

    @Override
    public StepVisitLogResponse updateVisitLog(Long visitLogId, StepVisitLogCreateRequest request, Long companyId) {
        Company company = getCompany(companyId);
        JobWorkflowStepVisitLog visitLog = visitLogRepository.findById(visitLogId)
                .orElseThrow(() -> new VisitLogNotFoundException("Visit log not found"));

        if (!visitLog.getLoggedBy().getId().equals(company.getUser().getId())) {
            throw new ForbiddenActionException("Not allowed to edit this visit log");
        }

        if (request.getVisitDate() != null)
            visitLog.setVisitDate(request.getVisitDate());
        if (request.getTimeIn() != null)
            visitLog.setTimeIn(request.getTimeIn());
        if (request.getTimeOut() != null)
            visitLog.setTimeOut(request.getTimeOut());
        if (request.getDescription() != null)
            visitLog.setDescription(request.getDescription());

        stepActivityService.log(
                visitLog.getStep(),
                company.getUser(),
                JobWorkflowStepActivityType.VISIT_UPDATED,
                "Updated a visit log for " + visitLog.getVisitDate());

        return map(visitLog);
    }

    @Override
    public void deleteVisitLog(Long visitLogId, Long companyId) {
        Company company = getCompany(companyId);
        JobWorkflowStepVisitLog visitLog = visitLogRepository.findById(visitLogId)
                .orElseThrow(() -> new VisitLogNotFoundException("Visit log not found"));

        if (!visitLog.getLoggedBy().getId().equals(company.getUser().getId())) {
            throw new ForbiddenActionException("Not allowed to delete this visit log");
        }

        stepActivityService.log(
                visitLog.getStep(),
                company.getUser(),
                JobWorkflowStepActivityType.VISIT_DELETED,
                "Deleted a visit log");

        visitLogRepository.delete(visitLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StepVisitLogResponse> getVisitLogs(Long stepId, Long companyId) {
        getStep(stepId, companyId); // Validates access
        return visitLogRepository.findByStepIdOrderByVisitDateDescTimeInDesc(stepId)
                .stream()
                .map(this::map)
                .toList();
    }

    private StepVisitLogResponse map(JobWorkflowStepVisitLog log) {
        return StepVisitLogResponse.builder()
                .id(log.getId())
                .visitDate(log.getVisitDate())
                .timeIn(log.getTimeIn())
                .timeOut(log.getTimeOut())
                .description(log.getDescription())
                .loggedById(log.getLoggedBy().getId())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}