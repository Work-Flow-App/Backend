package com.workflow.dto.auth;

import com.workflow.entity.auth.User;

public record UserLookupResult(User user, boolean isNew) {}
