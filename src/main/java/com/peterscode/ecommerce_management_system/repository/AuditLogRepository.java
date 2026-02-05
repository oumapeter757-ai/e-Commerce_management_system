package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserEmail(String userEmail, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByIpAddress(String ipAddress, Pageable pageable);

    Page<AuditLog> findByStatus(String status, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE a.userEmail = :userEmail " +
            "AND a.createdAt BETWEEN :startDate AND :endDate")
    List<AuditLog> findUserActivityInDateRange(
            @Param("userEmail") String userEmail,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.ipAddress = :ipAddress " +
            "AND a.status = 'FAILURE' AND a.createdAt > :since")
    Long countFailedAttemptsByIp(
            @Param("ipAddress") String ipAddress,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT a FROM AuditLog a WHERE a.action = :action " +
            "AND a.status = 'FAILURE' " +
            "AND a.createdAt > :since " +
            "ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentFailedActions(
            @Param("action") String action,
            @Param("since") LocalDateTime since
    );
}