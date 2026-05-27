package com.workflow.service.estimate;

import com.workflow.dto.estimate.*;

public interface IEstimateService {

    EstimateResponse getEstimate(Long estimateId, Long companyId);

    EstimateResponse getEstimateByJob(Long jobId, Long companyId);

    EstimateResponse updateEstimate(Long estimateId, EstimateUpdateRequest request, Long companyId);

    void deleteEstimate(Long estimateId, Long companyId);

    /** Create a new line item in the library and a job-scoped copy on the estimate. */
    EstimateResponse createAndLinkLineItem(Long estimateId, LineItemCreateRequest request, Long companyId);

    /** Copy an existing library line item onto the estimate as an isolated EstimateLineItem. */
    EstimateResponse linkExistingLineItem(Long estimateId, Long lineItemId, Long companyId);

    /** Update a job-scoped EstimateLineItem independently of the library. */
    EstimateResponse updateEstimateLineItem(Long estimateId, Long estimateLineItemId,
                                            LineItemUpdateRequest request, Long companyId);

    /** Update the approval status of a single job-scoped line item. Only APPROVED may be set via API. */
    EstimateResponse updateEstimateLineItemStatus(Long estimateId, Long estimateLineItemId,
                                                  LineItemStatusUpdateRequest request, Long companyId);

    /** Remove an EstimateLineItem from the estimate. */
    EstimateResponse unlinkLineItem(Long estimateId, Long estimateLineItemId, Long companyId);
}
