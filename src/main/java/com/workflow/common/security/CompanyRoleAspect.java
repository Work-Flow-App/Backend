package com.workflow.common.security;

import com.workflow.common.constant.CompanyRole;
import com.workflow.common.exception.business.ForbiddenActionException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class CompanyRoleAspect {

    @Around("@annotation(com.workflow.common.security.RequireCompanyRole)")
    public Object enforce(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        RequireCompanyRole annotation = sig.getMethod().getAnnotation(RequireCompanyRole.class);
        CompanyRole[] allowed = annotation.value();

        CompanyContext ctx = CompanyContextHolder.get();
        if (ctx == null) {
            throw new ForbiddenActionException("No company context — unauthenticated or non-company user");
        }

        boolean permitted = Arrays.asList(allowed).contains(ctx.companyRole());
        if (!permitted) {
            throw new ForbiddenActionException("Insufficient role: " + ctx.companyRole());
        }

        return pjp.proceed();
    }
}
