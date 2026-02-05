package com.peterscode.ecommerce_management_system.security;

import com.peterscode.ecommerce_management_system.constant.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile(SecurityConstants.PASSWORD_PATTERN);

    /**
     * Get currently authenticated user's username
     */
    public Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                return Optional.of(userDetails.getUsername());
            } else if (authentication.getPrincipal() instanceof String) {
                return Optional.of((String) authentication.getPrincipal());
            }
        }
        return Optional.empty();
    }

    /**
     * Get client IP address from request
     */
    public String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Check if IP is blacklisted
     */
    public boolean isIpBlacklisted(String ipAddress) {
        String key = SecurityConstants.IP_BLACKLIST_KEY + ipAddress;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Blacklist an IP address
     */
    public void blacklistIp(String ipAddress, long durationMillis) {
        String key = SecurityConstants.IP_BLACKLIST_KEY + ipAddress;
        redisTemplate.opsForValue().set(key, "blocked", durationMillis, TimeUnit.MILLISECONDS);
        log.warn("IP address blacklisted: {}", ipAddress);
    }

    /**
     * Record login attempt
     */
    public void recordLoginAttempt(String username, String ipAddress) {
        String key = SecurityConstants.LOGIN_ATTEMPTS_KEY + username + ":" + ipAddress;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, SecurityConstants.LOGIN_ATTEMPT_WINDOW, TimeUnit.MILLISECONDS);
    }

    /**
     * Get login attempt count
     */
    public int getLoginAttempts(String username, String ipAddress) {
        String key = SecurityConstants.LOGIN_ATTEMPTS_KEY + username + ":" + ipAddress;
        Object attempts = redisTemplate.opsForValue().get(key);
        return attempts != null ? Integer.parseInt(attempts.toString()) : 0;
    }

    /**
     * Reset login attempts
     */
    public void resetLoginAttempts(String username, String ipAddress) {
        String key = SecurityConstants.LOGIN_ATTEMPTS_KEY + username + ":" + ipAddress;
        redisTemplate.delete(key);
    }

    /**
     * Lock account temporarily
     */
    public void lockAccount(String username) {
        String key = SecurityConstants.ACCOUNT_LOCK_KEY + username;
        redisTemplate.opsForValue().set(
                key,
                "locked",
                SecurityConstants.ACCOUNT_LOCK_DURATION,
                TimeUnit.MILLISECONDS
        );
        log.warn("Account locked: {}", username);
    }

    /**
     * Check if account is locked
     */
    public boolean isAccountLocked(String username) {
        String key = SecurityConstants.ACCOUNT_LOCK_KEY + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Validate password strength
     */
    public boolean isPasswordStrong(String password) {
        if (password == null || password.length() < SecurityConstants.MIN_PASSWORD_LENGTH) {
            return false;
        }
        if (password.length() > SecurityConstants.MAX_PASSWORD_LENGTH) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Sanitize input to prevent XSS attacks
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[<>\"']", "");
    }

    /**
     * Check if request is from suspicious IP
     */
    public boolean isSuspiciousActivity(String ipAddress, String username) {
        int attempts = getLoginAttempts(username, ipAddress);
        return attempts >= SecurityConstants.MAX_LOGIN_ATTEMPTS;
    }

    /**
     * Generate secure random token
     */
    public String generateSecureToken() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}