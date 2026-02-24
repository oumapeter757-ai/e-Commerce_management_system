package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.Payment;
import com.peterscode.ecommerce_management_system.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Basic CRUD operations
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByCheckoutRequestId(String checkoutRequestId);
    Optional<Payment> findByMerchantRequestId(String merchantRequestId);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findByUserId(Long userId);
    Page<Payment> findByUserId(Long userId, Pageable pageable);
    List<Payment> findByStatus(PaymentStatus status);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    List<Payment> findByPaymentMethod(String paymentMethod);


    // Complex queries
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Payment p WHERE p.order.id = :orderId AND p.status IN :statuses")
    boolean existsByOrderIdAndStatusIn(@Param("orderId") Long orderId,
                                       @Param("statuses") Collection<PaymentStatus> statuses);

    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.status = 'SUCCESSFUL'")
    List<Payment> findSuccessfulPaymentsByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.user.id = :userId AND p.status = 'SUCCESSFUL'")
    BigDecimal getTotalSuccessfulAmountByUser(@Param("userId") Long userId);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PROCESSING' " +
            "AND p.createdAt <= :timeoutThreshold")
    List<Payment> findProcessingPaymentsOlderThan(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.order.id = :orderId AND p.status = 'SUCCESSFUL'")
    BigDecimal getTotalPaidAmountForOrder(@Param("orderId") Long orderId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUCCESSFUL' " +
            "AND DATE(p.createdAt) = CURRENT_DATE")
    Long countSuccessfulPaymentsToday();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.status = 'SUCCESSFUL' AND DATE(p.createdAt) = CURRENT_DATE")
    BigDecimal getTodayRevenue();

    // Concurrency control
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :paymentId")
    Optional<Payment> findByIdWithLock(@Param("paymentId") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId")
    Optional<Payment> findByOrderIdWithLock(@Param("orderId") Long orderId);

    // Search operations
    @Query("SELECT p FROM Payment p WHERE p.order.orderNumber LIKE %:orderNumber%")
    List<Payment> searchByOrderNumber(@Param("orderNumber") String orderNumber);

    @Query("SELECT p FROM Payment p WHERE " +
            "(:userId IS NULL OR p.user.id = :userId) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(p.createdAt BETWEEN COALESCE(:startDate, p.createdAt) AND COALESCE(:endDate, p.createdAt))")
    Page<Payment> searchPayments(@Param("userId") Long userId,
                                 @Param("status") PaymentStatus status,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate,
                                 Pageable pageable);
}