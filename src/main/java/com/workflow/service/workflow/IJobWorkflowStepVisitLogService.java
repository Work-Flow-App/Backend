package com.workflow.service.workflow;

import com.workflow.dto.workflow.StepVisitLogCreateRequest;
import com.workflow.dto.workflow.StepVisitLogResponse;
import com.workflow.dto.workflow.StepVisitLogSummaryResponse;

import java.util.List;

public interface IJobWorkflowStepVisitLogService {
    StepVisitLogResponse addVisitLog(Long stepId, StepVisitLogCreateRequest request, Long companyId);

    StepVisitLogResponse updateVisitLog(Long visitLogId, StepVisitLogCreateRequest request, Long companyId);

    void deleteVisitLog(Long visitLogId, Long companyId);

    StepVisitLogSummaryResponse getVisitLogs(Long stepId, Long companyId);
}