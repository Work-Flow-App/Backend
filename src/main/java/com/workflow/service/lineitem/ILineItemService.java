package com.workflow.service.lineitem;

import com.workflow.dto.estimate.LineItemCreateRequest;
import com.workflow.dto.estimate.LineItemResponse;
import com.workflow.dto.estimate.LineItemUpdateRequest;

import java.util.List;

public interface ILineItemService {

    LineItemResponse createLineItem(LineItemCreateRequest request, Long companyId);

    LineItemResponse getLineItem(Long id, Long companyId);

    List<LineItemResponse> getAllLineItems(Long companyId);

    LineItemResponse updateLineItem(Long id, LineItemUpdateRequest request, Long companyId);

    void deleteLineItem(Long id, Long companyId);
}
