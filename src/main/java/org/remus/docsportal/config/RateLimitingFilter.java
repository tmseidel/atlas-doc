package org.remus.docsportal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate-limiter for login attempts (AUTH-005).
 * Blocks IPs after consecutive failed login attempts.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> blockedUntil = new ConcurrentHashMap<>();

    @Value("${docs-portal.security.max-failed-attempts:10}")
    private int maxFailedAttempts;

    @Value("${docs-portal.security.block-duration-minutes:15}")
    private int blockDurationMinutes;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIp(request);

        // Check if blocked
        Instant blocked = blockedUntil.get(ip);
        if (blocked != null && Instant.now().isBefore(blocked)) {
            response.setStatus(429);
            response.getWriter().write("Too many login attempts. Try again later.");
            return;
        } else if (blocked != null) {
            blockedUntil.remove(ip);
            failedAttempts.remove(ip);
        }

        // Track failed POST /login attempts
        if ("POST".equals(request.getMethod()) && "/login".equals(request.getRequestURI())) {
            request.setAttribute("rate-limit-ip", ip);
            request.setAttribute("rate-limit-check", true);
        }

        filterChain.doFilter(request, response);

        // The configured authentication failure handler marks failed logins before
        // issuing its redirect, which is more reliable than inspecting a 302 status.
        if ("POST".equals(request.getMethod()) && "/login".equals(request.getRequestURI())) {
            if (Boolean.TRUE.equals(request.getAttribute("login-failed"))) {
                AtomicInteger count = failedAttempts.computeIfAbsent(ip, k -> new AtomicInteger(0));
                if (count.incrementAndGet() >= maxFailedAttempts) {
                    blockedUntil.put(ip, Instant.now().plus(Duration.ofMinutes(blockDurationMinutes)));
                }
            } else {
                failedAttempts.remove(ip);
                blockedUntil.remove(ip);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
