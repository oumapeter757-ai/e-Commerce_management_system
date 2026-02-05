package com.peterscode.ecommerce_management_system.model.dto.response;

import com.peterscode.ecommerce_management_system.model.enums.PaymentMethod;
import com.peterscode.ecommerce_management_system.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private String transactionId;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String paymentGateway;
    private String payerEmail;
    private String payerName;
    private LocalDateTime paidAt;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Simplified fields for customer
    private Boolean isPaid;
    private Boolean isPending;
    private Boolean isFailed;
}
