package com.peterscode.ecommerce_management_system.model.entity;

import com.peterscode.ecommerce_management_system.model.enums.PaymentMethod;
import com.peterscode.ecommerce_management_system.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"order", "user"})
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order_id", columnList = "order_id"),
        @Index(name = "idx_payment_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_created_at", columnList = "created_at")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // Stores M-Pesa Receipt Number or Stripe Payment Intent ID
    @Column(name = "transaction_id")
    private String transactionId;

    // Used for M-Pesa async callbacks to match requests
    @Column(name = "merchant_request_id")
    private String merchantRequestId;

    @Column(name = "checkout_request_id")
    private String checkoutRequestId;

    // Getter and setter for mpesaReceiptNumber
    // M-Pesa receipt number
    @Column(name = "mpesa_receipt_number")
    private String mpesaReceiptNumber;

    @Column(name = "payment_details", columnDefinition = "TEXT")
    private String paymentDetails;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "callback_metadata", columnDefinition = "TEXT")
    private String callbackMetadata;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;


    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== BUSINESS LOGIC METHODS =====

    public void markAsProcessing(String checkoutRequestId, String merchantRequestId) {
        this.status = PaymentStatus.PROCESSING;
        this.checkoutRequestId = checkoutRequestId;
        this.merchantRequestId = merchantRequestId;
        this.transactionId = checkoutRequestId;
    }

    public void markAsSuccessful(String mpesaReceiptNumber, String phoneNumber, String callbackMetadata) {
        this.status = PaymentStatus.SUCCESSFUL;
        this.mpesaReceiptNumber = mpesaReceiptNumber;
        this.transactionId = mpesaReceiptNumber;
        this.phoneNumber = phoneNumber;
        this.callbackMetadata = callbackMetadata;
        this.errorMessage = null;
    }

    public void markAsFailed(String errorMessage) {
        this.status = PaymentStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markAsRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }

    public void markAsCancelled(String reason) {
        this.status = PaymentStatus.CANCELLED;
        this.errorMessage = reason;
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESSFUL;
    }

}