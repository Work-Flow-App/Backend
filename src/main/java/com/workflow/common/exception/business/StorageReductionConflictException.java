package com.workflow.common.exception.business;

import com.workflow.common.exception.base.ConflictException;

/**
 * Thrown when a requested decrease in extraStorageBlocks would drop the effective storage limit
 * below the company's currently used storage. Distinct from StorageLimitExceededException (402,
 * "you need to buy more storage to proceed") — this is a 409: the request is well-formed, but
 * conflicts with current resource usage.
 */
public class StorageReductionConflictException extends ConflictException {
    public StorageReductionConflictException(String message) {
        super(message);
    }
}
