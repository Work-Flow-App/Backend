package com.workflow.common.constant.validator;

import com.workflow.common.constant.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RoleValidator implements ConstraintValidator<ValidRole, Role> {

    @Override
    public boolean isValid(Role role, ConstraintValidatorContext context) {
        if (role == null) return false;
        return role == Role.COMPANY || role == Role.WORKER;
    }
}

