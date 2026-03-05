package com.workflow.service.estimate;

import com.workflow.dto.estimate.*;

public interface IEstimateService {

    EstimateResponse getEstimate(Long estimateId, Long companyId);

    EstimateResponse getEstimateByJob(Long jobId, Long companyId);

    EstimateResponse updateEstimate(Long estimateId, EstimateUpdateRequest request, Long companyId);

    void deleteEstimate(Long estimateId, Long companyId);

    /** Create a new line item and immediately link it to the estimate. */
    EstimateResponse createAndLinkLineItem(Long estimateId, LineItemCreateRequest request, Long companyId);

    /** Link an already-existing line item to the estimate. */
    EstimateResponse linkExistingLineItem(Long estimateId, Long lineItemId, Long companyId);

    /** Remove a line item from the estimate without deleting the line item itself. */
    EstimateResponse unlinkLineItem(Long estimateId, Long lineItemId, Long companyId);
}
