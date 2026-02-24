package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.request.MPesaCallbackRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.PaymentRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.MPesaQueryResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PaymentService {

    // M-Pesa Payment Operations
    PaymentResponse initiatePayment(PaymentRequest request, Long userId);
    void handleMPesaCallback(MPesaCallbackRequest callbackRequest,
                             String rawPayload,
                             String clientIp,
                             String signature);
    void handleMPesaTimeout(Map<String, Object> timeoutData);
    MPesaQueryResponse queryMpesaTransaction(String checkoutRequestId);

    // Payment Management
    PaymentResponse getPaymentStatusByOrder(Long orderId, Long userId);
    PaymentResponse getPaymentById(Long paymentId);
    List<PaymentResponse> getUserPayments(Long userId);
    Page<PaymentResponse> getAllPayments(Pageable pageable);

    // Refund & Admin Operations
    PaymentResponse processRefund(Long paymentId, BigDecimal amount, String reason);
    void cancelPayment(Long paymentId, String reason);

    // Utility Methods
    boolean verifyPayment(Long orderId);
    BigDecimal getTotalPaidAmount(Long orderId);
}