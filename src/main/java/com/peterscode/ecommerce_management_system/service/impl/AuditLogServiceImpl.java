package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.model.audit.AuditLog;
import com.peterscode.ecommerce_management_system.repository.AuditLogRepository;
import com.peterscode.ecommerce_management_system.security.SecurityUtils;
import com.peterscode.ecommerce_management_system.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;

    @Async
    @Override
    @Transactional
    public void logSuccess(String action, String resourceType, String resourceId,
                           String details, HttpServletRequest request) {
        try {
            AuditLog auditLog = buildAuditLog(action, resourceType, resourceId,
                    "SUCCESS", details, null, request);
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for successful action: {}", action);
        } catch (Exception e) {
            log.error("Error creating audit log: {}", e.getMessage());
        }
    }

    @Async
    @Override
    @Transactional
    public void logFailure(String action, String resourceType, String resourceId,
                           String errorMessage, HttpServletRequest request) {
        try {
            AuditLog auditLog = buildAuditLog(action, resourceType, resourceId,
                    "FAILURE", null, errorMessage, request);
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for failed action: {}", action);
        } catch (Exception e) {
            log.error("Error creating audit log: {}", e.getMessage());
        }
    }

    @Async
    @Override
    @Transactional
    public void logWarning(String action, String resourceType, String resourceId,
                           String details, HttpServletRequest request) {
        try {
            AuditLog auditLog = buildAuditLog(action, resourceType, resourceId,
                    "WARNING", details, null, request);
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for warning: {}", action);
        } catch (Exception e) {
            log.error("Error creating audit log: {}", e.getMessage());
        }
    }

    @Async
    @Override
    @Transactional
    public void logAuthenticationAttempt(String username, boolean success,
                                         String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userEmail(username)
                    .action(success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE")
                    .resourceType("AUTHENTICATION")
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(success ? "SUCCESS" : "FAILURE")
                    .details(success ? "User logged in successfully" : "Failed login attempt")
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Authentication attempt logged for user: {}", username);
        } catch (Exception e) {
            log.error("Error logging authentication attempt: {}", e.getMessage());
        }
    }

    @Async
    @Override
    @Transactional
    public void logLogout(String username, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userEmail(username)
                    .action("LOGOUT")
                    .resourceType("AUTHENTICATION")
                    .ipAddress(ipAddress)
                    .status("SUCCESS")
                    .details("User logged out")
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Logout logged for user: {}", username);
        } catch (Exception e) {
            log.error("Error logging logout: {}", e.getMessage());
        }
    }

    @Async
    @Override
    @Transactional
    public void logPasswordChange(String username, boolean success, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userEmail(username)
                    .action("PASSWORD_CHANGE")
                    .resourceType("USER_SECURITY")
                    .ipAddress(ipAddress)
                    .status(success ? "SUCCESS" : "FAILURE")
                    .details(success ? "Password changed successfully" : "Password change failed")
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Password change logged for user: {}", username);
        } catch (Exception e) {
            log.error("Error logging password change: {}", e.getMessage());
        }
    }

    @Async
    @Override
    @Transactional
    public void logSecurityEvent(String event, String details, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(event)
                    .resourceType("SECURITY")
                    .ipAddress(ipAddress)
                    .status("WARNING")
                    .details(details)
                    .build();

            auditLogRepository.save(auditLog);
            log.warn("Security event logged: {} from IP: {}", event, ipAddress);
        } catch (Exception e) {
            log.error("Error logging security event: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(String userEmail, Pageable pageable) {
        return auditLogRepository.findByUserEmail(userEmail, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByIp(String ipAddress, Pageable pageable) {
        return auditLogRepository.findByIpAddress(ipAddress, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate,
                                                  LocalDateTime endDate,
                                                  Pageable pageable) {
        return auditLogRepository.findByDateRange(startDate, endDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getFailedLoginAttempts(String ipAddress, LocalDateTime since) {
        return auditLogRepository.countFailedAttemptsByIp(ipAddress, since);
    }

    /**
     * Build audit log from request
     */
    private AuditLog buildAuditLog(String action, String resourceType, String resourceId,
                                   String status, String details, String errorMessage,
                                   HttpServletRequest request) {
        String userEmail = securityUtils.getCurrentUsername().orElse("anonymous");
        String ipAddress = securityUtils.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        return AuditLog.builder()
                .userEmail(userEmail)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestMethod(request.getMethod())
                .requestUrl(request.getRequestURI())
                .status(status)
                .details(details)
                .errorMessage(errorMessage)
                .build();
    }
}