package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.response.PaymentResponse;
import com.peterscode.ecommerce_management_system.model.entity.Payment;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.model.enums.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        // 1. Safely extract User details
        String payerName = "Unknown";
        String payerEmail = "Unknown";

        if (payment.getUser() != null) {
            payerName = formatFullName(payment.getUser());
            payerEmail = payment.getUser().getEmail();
        }

        // 2. Safely extract Order details
        Long orderId = null;
        String orderNumber = null;

        if (payment.getOrder() != null) {
            orderId = payment.getOrder().getId();
            orderNumber = payment.getOrder().getOrderNumber();
        }

        // 3. Determine Status Flags (Helper booleans for frontend)
        boolean isPaid = payment.getStatus() == PaymentStatus.SUCCESSFUL || payment.getStatus() == PaymentStatus.COMPLETED;
        boolean isFailed = payment.getStatus() == PaymentStatus.FAILED;
        boolean isPending = payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.PROCESSING;

        // 4. Build Response
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(orderId)
                .orderNumber(orderNumber)
                .transactionId(payment.getTransactionId())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency("KES") // Default to KES
                .paymentGateway(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "M-PESA")
                .payerEmail(payerEmail)
                .payerName(payerName)

                // Logic: Only show paid date if actually paid
                .paidAt(isPaid ? payment.getUpdatedAt() : null)

                // Logic: Generic failure message since specific reason column was removed
                .failureReason(isFailed ? "Transaction failed or was cancelled" : null)

                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())

                // Boolean flags
                .isPaid(isPaid)
                .isPending(isPending)
                .isFailed(isFailed)
                .build();
    }

    /**
     * Helper to concatenate First and Last name from User entity.
     */
    private String formatFullName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";

        if (first.isEmpty() && last.isEmpty()) {
            return "Unknown";
        }

        return (first + " " + last).trim();
    }
}