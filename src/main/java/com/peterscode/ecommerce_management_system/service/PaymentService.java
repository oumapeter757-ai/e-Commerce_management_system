package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.request.MPesaCallbackRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.PaymentRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PaymentService {

    /**
     * Initiates an STK Push to the customer's phone.
     *
     * @param request Payment request containing order ID and amount
     * @param userId ID of the user making the payment
     * @return Payment response with transaction details
     */
    PaymentResponse initiatePayment(PaymentRequest request, Long userId);

    /**
     * Processes the asynchronous callback from Safaricom.
     *
     * @param callbackRequest The deserialized callback object for easy data access
     * @param rawPayload The raw JSON string for HMAC signature verification
     * @param clientIp The IP address of the caller (for IP whitelisting)
     * @param signature The X-Signature header value (for HMAC verification)
     */
    void handleMPesaCallback(MPesaCallbackRequest callbackRequest, String rawPayload, String clientIp, String signature);

    /**
     * Handles M-PESA timeout callback
     * Called when the STK Push times out (user doesn't respond)
     *
     * @param timeoutData Timeout data from M-PESA
     */
    void handleMPesaTimeout(Map<String, Object> timeoutData);

    /**
     * Get payment status by order ID
     *
     * @param orderId Order ID
     * @param userId User ID (for authorization)
     * @return Payment response
     */
    PaymentResponse getPaymentStatusByOrder(Long orderId, Long userId);

    /**
     * Get payment by ID
     *
     * @param paymentId Payment ID
     * @return Payment response
     */
    PaymentResponse getPaymentById(Long paymentId);

    /**
     * Get all payments for a user
     *
     * @param userId User ID
     * @return List of payment responses
     */
    List<PaymentResponse> getUserPayments(Long userId);

    /**
     * Get all payments (Admin only)
     *
     * @param pageable Pagination parameters
     * @return Page of payment responses
     */
    Page<PaymentResponse> getAllPayments(Pageable pageable);

    /**
     * Process refund (Admin only)
     *
     * @param paymentId Payment ID
     * @param amount Refund amount
     * @param reason Refund reason
     * @return Updated payment response
     */
    PaymentResponse processRefund(Long paymentId, BigDecimal amount, String reason);
}