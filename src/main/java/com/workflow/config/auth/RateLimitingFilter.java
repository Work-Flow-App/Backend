package com.workflow.config.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitingFilter extends OncePerRequestFilter {

    // Paths and their max attempts per window
    private static final Map<String, Integer> RATE_LIMITED_PATHS = Map.of(
        "/api/v1/auth/login",               5,
        "/api/v1/auth/forgot-password",     3,
        "/api/v1/auth/reset-password",      5,
        "/api/v1/auth/resend-verification", 3
    );

    private static final long WINDOW_MS = 60_000L; // 1 minute

    // key: "ip::path", value: [count, windowStartMs]
    private final ConcurrentHashMap<String, long[]> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        Integer maxAttempts = RATE_LIMITED_PATHS.get(path);

        if (maxAttempts != null) {
            String ip = extractIp(request);
            String key = ip + "::" + path;
            long now = Instant.now().toEpochMilli();

            long[] entry = counters.compute(key, (k, v) -> {
                if (v == null || now - v[1] > WINDOW_MS) {
                    return new long[]{1, now};
                }
                v[0]++;
                return v;
            });

            if (entry[0] > maxAttempts) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many attempts. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
