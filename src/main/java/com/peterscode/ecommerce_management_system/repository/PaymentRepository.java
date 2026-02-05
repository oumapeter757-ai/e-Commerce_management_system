package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.Payment;
import com.peterscode.ecommerce_management_system.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Finds a payment by the Transaction ID.
     * In the initial phase of M-PESA payment, this stores the CheckoutRequestID.
     * On completion, it is updated to the M-PESA Receipt Number.
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Checks if a payment exists for a specific order with any of the given statuses.
     * We use a custom query here because 'order' is an entity relationship,
     * but we are querying by the scalar 'id'.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Payment p WHERE p.order.id = :orderId AND p.status IN :statuses")
    boolean existsByOrderIdAndStatusIn(@Param("orderId") Long orderId, @Param("statuses") Collection<PaymentStatus> statuses);

    /**
     * Additional helper to find payment by Order ID directly.
     * Useful for admin views or order history.
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Specific lookup for CheckoutRequestID if distinct from transactionId
     * (Useful for debugging logs).
     */
    Optional<Payment> findByCheckoutRequestId(String checkoutRequestId);

    List<Payment> findByOrderUserId(Long userId);
}