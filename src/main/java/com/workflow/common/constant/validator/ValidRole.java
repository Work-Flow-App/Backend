package com.workflow.common.constant.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RoleValidator.class)
@Documented
public @interface ValidRole {
    String message() default "Role must be COMPANY or WORKER";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

