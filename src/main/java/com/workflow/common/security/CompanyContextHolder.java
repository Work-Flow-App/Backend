package com.workflow.common.security;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public final class CompanyContextHolder {

    private static final String KEY = "companyCtx";

    private CompanyContextHolder() {}

    public static void set(CompanyContext ctx) {
        RequestContextHolder.currentRequestAttributes()
                .setAttribute(KEY, ctx, RequestAttributes.SCOPE_REQUEST);
    }

    public static CompanyContext get() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        return (CompanyContext) attrs.getAttribute(KEY, RequestAttributes.SCOPE_REQUEST);
    }

    public static Long getCompanyId() {
        CompanyContext ctx = get();
        if (ctx == null) return null;
        return ctx.companyId();
    }
}
