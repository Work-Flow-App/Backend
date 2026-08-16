package com.workflow.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPagedResponse<T> {
    private List<T> data;
    private Long nextCursor; // The ID to pass to get the next page
    private boolean hasNext; // True if there are more items
}