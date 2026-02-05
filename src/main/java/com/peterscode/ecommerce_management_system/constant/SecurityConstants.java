package com.peterscode.ecommerce_management_system.constant;

public class SecurityConstants {

    // JWT Configuration
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final long ACCESS_TOKEN_VALIDITY = 3600000; // 1 hour in milliseconds
    public static final long REFRESH_TOKEN_VALIDITY = 604800000; // 7 days in milliseconds

    // Rate Limiting
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final long LOGIN_ATTEMPT_WINDOW = 900000; // 15 minutes in milliseconds
    public static final long ACCOUNT_LOCK_DURATION = 1800000; // 30 minutes in milliseconds

    // Password Policy
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    // Session Configuration
    public static final int MAX_SESSIONS_PER_USER = 3;
    public static final long SESSION_TIMEOUT = 1800000; // 30 minutes

    // IP Whitelist/Blacklist
    public static final String IP_BLACKLIST_KEY = "security:ip:blacklist:";
    public static final String IP_WHITELIST_KEY = "security:ip:whitelist:";

    // Brute Force Protection
    public static final String LOGIN_ATTEMPTS_KEY = "security:login:attempts:";
    public static final String ACCOUNT_LOCK_KEY = "security:account:lock:";

    // CORS
    public static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:4200",
            "https://yourdomain.com"
    };

    // Public Endpoints (No Authentication Required)
    public static final String[] PUBLIC_URLS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health"
    };

    // Admin Only Endpoints
    public static final String[] ADMIN_URLS = {
            "/api/v1/users/admin/**",
            "/api/v1/reports/**",
            "/api/v1/audit/**"
    };

    private SecurityConstants() {
        throw new IllegalStateException("Constants class");
    }
}