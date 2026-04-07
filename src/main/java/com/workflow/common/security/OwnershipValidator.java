package com.workflow.common.security;

import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.entity.auth.User;

public final class OwnershipValidator {

    private OwnershipValidator() {
    }

    public static void validateOwner(Long ownerId, User currentUser, String resource) {
        if (!ownerId.equals(currentUser.getId())) {
            throw new ForbiddenActionException(
                    "You are not allowed to modify this " + resource);
        }
    }
}
