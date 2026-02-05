package com.peterscode.ecommerce_management_system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityUtils securityUtils;

    private static final String RATE_LIMIT_KEY_PREFIX = "security:ratelimit:";
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_REQUESTS_PER_HOUR = 1000;
    private static final long MINUTE_IN_SECONDS = 60;
    private static final long HOUR_IN_SECONDS = 3600;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String ipAddress = securityUtils.getClientIpAddress(request);
        String path = request.getRequestURI();

        // Check if IP is blacklisted
        if (securityUtils.isIpBlacklisted(ipAddress)) {
            log.warn("Blocked request from blacklisted IP: {}", ipAddress);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"Your IP has been blocked\"}");
            return;
        }

        // Apply rate limiting
        if (!isRateLimitAllowed(ipAddress, path)) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", ipAddress, path);
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if request is allowed based on rate limits
     */
    private boolean isRateLimitAllowed(String ipAddress, String path) {
        // Per-minute rate limit
        String minuteKey = RATE_LIMIT_KEY_PREFIX + "minute:" + ipAddress;
        Long minuteCount = redisTemplate.opsForValue().increment(minuteKey);

        if (minuteCount != null && minuteCount == 1) {
            redisTemplate.expire(minuteKey, MINUTE_IN_SECONDS, TimeUnit.SECONDS);
        }

        if (minuteCount != null && minuteCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Minute rate limit exceeded for IP: {}", ipAddress);
            return false;
        }

        // Per-hour rate limit
        String hourKey = RATE_LIMIT_KEY_PREFIX + "hour:" + ipAddress;
        Long hourCount = redisTemplate.opsForValue().increment(hourKey);

        if (hourCount != null && hourCount == 1) {
            redisTemplate.expire(hourKey, HOUR_IN_SECONDS, TimeUnit.SECONDS);
        }

        if (hourCount != null && hourCount > MAX_REQUESTS_PER_HOUR) {
            log.warn("Hour rate limit exceeded for IP: {}", ipAddress);
            // Auto-blacklist for 30 minutes if hourly limit is exceeded
            securityUtils.blacklistIp(ipAddress, 30 * 60 * 1000L);
            return false;
        }

        // Stricter rate limiting for sensitive endpoints
        if (isSensitiveEndpoint(path)) {
            String sensitiveKey = RATE_LIMIT_KEY_PREFIX + "sensitive:" + ipAddress;
            Long sensitiveCount = redisTemplate.opsForValue().increment(sensitiveKey);

            if (sensitiveCount != null && sensitiveCount == 1) {
                redisTemplate.expire(sensitiveKey, MINUTE_IN_SECONDS, TimeUnit.SECONDS);
            }

            if (sensitiveCount != null && sensitiveCount > 10) { // Max 10 requests per minute for sensitive endpoints
                log.warn("Sensitive endpoint rate limit exceeded for IP: {}", ipAddress);
                return false;
            }
        }

        return true;
    }

    /**
     * Check if endpoint is sensitive (login, register, password reset)
     */
    private boolean isSensitiveEndpoint(String path) {
        return path.contains("/auth/login") ||
                path.contains("/auth/register") ||
                path.contains("/auth/forgot-password") ||
                path.contains("/auth/reset-password") ||
                path.contains("/payments/");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip rate limiting for health checks and static resources
        return path.startsWith("/actuator/health") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }
}