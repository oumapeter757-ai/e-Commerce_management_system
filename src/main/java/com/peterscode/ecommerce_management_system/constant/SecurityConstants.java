package com.peterscode.ecommerce_management_system.constant;

/**
 * Security Constants
 *
 * Central location for all security-related configuration constants
 */
public final class SecurityConstants {

    // ==================== JWT CONFIGURATION ====================

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final long ACCESS_TOKEN_VALIDITY = 3600000; // 1 hour in milliseconds
    public static final long REFRESH_TOKEN_VALIDITY = 604800000; // 7 days in milliseconds

    // ==================== RATE LIMITING ====================

    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final long LOGIN_ATTEMPT_WINDOW = 900000; // 15 minutes in milliseconds
    public static final long ACCOUNT_LOCK_DURATION = 1800000; // 30 minutes in milliseconds

    // ==================== PASSWORD POLICY ====================

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;

    /**
     * Password Pattern:
     * - At least one digit [0-9]
     * - At least one lowercase [a-z]
     * - At least one uppercase [A-Z]
     * - At least one special character [@#$%^&+=!]
     * - No whitespace
     * - Minimum 8 characters
     */
    public static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    // ==================== SESSION CONFIGURATION ====================

    public static final int MAX_SESSIONS_PER_USER = 3;
    public static final long SESSION_TIMEOUT = 1800000; // 30 minutes in milliseconds

    // ==================== REDIS KEYS ====================

    public static final String IP_BLACKLIST_KEY = "security:ip:blacklist:";
    public static final String IP_WHITELIST_KEY = "security:ip:whitelist:";
    public static final String LOGIN_ATTEMPTS_KEY = "security:login:attempts:";
    public static final String ACCOUNT_LOCK_KEY = "security:account:lock:";
    public static final String REFRESH_TOKEN_KEY = "security:refresh:token:";
    public static final String VERIFICATION_CODE_KEY = "security:verification:";

    // ==================== CORS CONFIGURATION ====================

    /**
     * Allowed origins for CORS
     * UPDATE THIS FOR PRODUCTION with your actual domain
     */
    public static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",           // React development
            "http://localhost:4200",           // Angular development
            "http://localhost:8080",           // Vue development
            "https://yourdomain.com",          // Production frontend
            "https://www.yourdomain.com",      // Production frontend (www)
            "https://admin.yourdomain.com"     // Admin panel
    };

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Endpoints that don't require authentication
     */
    public static final String[] PUBLIC_URLS = {
            // Authentication
            "/api/v1/auth/login",
            "/api/v1/auth/register/**",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email/**",
            "/api/v1/auth/resend-verification",

            // Documentation
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",

            // Health checks
            "/actuator/health",
            "/actuator/info",

            // Public product viewing
            "/api/v1/products/**",
            "/api/v1/categories/**",
            "/api/v1/inventory/**",

            // Guest cart
            "/api/v1/cart/guest/**",

            // Public reviews
            "/api/v1/reviews/product/**",

            // Public flash deals
            "/api/v1/flash-deals/**",

            // Public coupon validation
            "/api/v1/coupons/validate",

            // M-PESA callbacks (CRITICAL - must be public)
            "/api/v1/payments/mpesa/callback",
            "/api/v1/payments/mpesa/timeout"
    };

    // ==================== ADMIN ONLY ENDPOINTS ====================

    /**
     * Endpoints restricted to ADMIN role only
     */
    public static final String[] ADMIN_URLS = {
            // User management
            "/api/v1/auth/admin/**",
            "/api/v1/users/admin/**",

            // Reports & analytics
            "/api/v1/reports/**",
            "/api/v1/analytics/**",
            "/api/v1/audit/**",

            // Order management (admin functions)
            "/api/v1/orders/status/**",
            "/api/v1/orders/user/**",

            // Payment management (admin functions)
            "/api/v1/payments/refund/**",
            "/api/v1/payments/admin/**"
    };

    // ==================== USER (CUSTOMER) ONLY ENDPOINTS ====================

    /**
     * Endpoints restricted to USER (CUSTOMER) role
     */
    public static final String[] USER_URLS = {
            // Cart
            "/api/v1/cart/**",

            // Addresses
            "/api/v1/addresses/**",

            // Orders (creation)
            "/api/v1/orders/create",
            "/api/v1/orders/my-orders/**",

            // Payments (initiation)
            "/api/v1/payments/initiate",
            "/api/v1/payments/user/**"
    };

    // ==================== ROLES ====================

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SELLER = "SELLER";
    public static final String ROLE_SYSTEM = "SYSTEM";

    // ==================== SECURITY HEADERS ====================

    public static final String CSP_HEADER =
            "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self' data:; " +
                    "connect-src 'self'; " +
                    "frame-ancestors 'none'; " +
                    "form-action 'self'";

    // ==================== TOKEN EXPIRY ====================

    public static final long EMAIL_VERIFICATION_EXPIRY = 86400000; // 24 hours
    public static final long PASSWORD_RESET_EXPIRY = 3600000; // 1 hour
    public static final long OTP_EXPIRY = 300000; // 5 minutes

    // ==================== ENCRYPTION ====================

    public static final String ENCRYPTION_ALGORITHM = "AES";
    public static final String ENCRYPTION_MODE = "AES/CBC/PKCS5Padding";
    public static final int ENCRYPTION_KEY_SIZE = 256;

    // ==================== AUDIT ====================

    public static final String[] AUDITED_ACTIONS = {
            "LOGIN",
            "LOGOUT",
            "REGISTER",
            "PASSWORD_CHANGE",
            "PASSWORD_RESET",
            "EMAIL_CHANGE",
            "PROFILE_UPDATE",
            "ORDER_CREATE",
            "PAYMENT_INITIATE",
            "REFUND_PROCESS",
            "USER_DELETE",
            "ADMIN_ACTION"
    };

    // ==================== VALIDATION ====================

    public static final String EMAIL_PATTERN =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public static final String PHONE_PATTERN =
            "^(\\+254|254|0)?[17]\\d{8}$"; // Kenyan phone number

    public static final String USERNAME_PATTERN =
            "^[a-zA-Z0-9._-]{3,20}$";

    // ==================== FILE UPLOAD ====================

    public static final long MAX_FILE_SIZE = 5242880; // 5MB in bytes
    public static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    };

    public static final String[] ALLOWED_DOCUMENT_TYPES = {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    };

    // ==================== M-PESA SECURITY ====================

    /**
     * Official Safaricom M-PESA callback IP addresses
     * IMPORTANT: Keep this updated with official IPs
     */
    public static final String[] MPESA_ALLOWED_IPS = {
            "196.201.214.200",
            "196.201.214.206",
            "196.201.213.114",
            "196.201.214.207",
            "196.201.214.208",
            "196.201.213.44",
            "196.201.212.127",
            "196.201.212.128",
            "196.201.212.129",
            "196.201.212.136",
            "196.201.212.138",
            "196.201.214.130"
    };

    private SecurityConstants() {
        throw new IllegalStateException("Constants class cannot be instantiated");
    }
}