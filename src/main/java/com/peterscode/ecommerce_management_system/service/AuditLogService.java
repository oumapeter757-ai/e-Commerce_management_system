package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.audit.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogService {

    /**
     * Log a successful action
     */
    void logSuccess(String action, String resourceType, String resourceId,
                    String details, HttpServletRequest request);

    /**
     * Log a failed action
     */
    void logFailure(String action, String resourceType, String resourceId,
                    String errorMessage, HttpServletRequest request);

    /**
     * Log a warning
     */
    void logWarning(String action, String resourceType, String resourceId,
                    String details, HttpServletRequest request);

    /**
     * Log authentication attempt
     */
    void logAuthenticationAttempt(String username, boolean success,
                                  String ipAddress, String userAgent);

    /**
     * Log logout event
     */
    void logLogout(String username, String ipAddress);

    /**
     * Log password change
     */
    void logPasswordChange(String username, boolean success, String ipAddress);

    /**
     * Log security event (suspicious activity, brute force, etc.)
     */
    void logSecurityEvent(String event, String details, String ipAddress);

    /**
     * Get audit logs by user
     */
    Page<AuditLog> getAuditLogsByUser(String userEmail, Pageable pageable);

    /**
     * Get audit logs by action
     */
    Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable);

    /**
     * Get audit logs by IP address
     */
    Page<AuditLog> getAuditLogsByIp(String ipAddress, Pageable pageable);

    /**
     * Get audit logs by date range
     */
    Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate,
                                           LocalDateTime endDate,
                                           Pageable pageable);

    /**
     * Get failed login attempts count
     */
    Long getFailedLoginAttempts(String ipAddress, LocalDateTime since);
}