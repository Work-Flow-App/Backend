package com.workflow.service.subscription;

public interface ISeatLimitService {

    /**
     * Throws SeatLimitExceededException if the company is already at or over its effective
     * worker-seat limit (active workers + pending invitations, since pending invites could
     * otherwise be stacked past the cap and accepted all at once).
     */
    void assertCapacity(Long companyId);
}
