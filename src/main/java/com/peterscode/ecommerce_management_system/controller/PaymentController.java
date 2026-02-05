package com.peterscode.ecommerce_management_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.MPesaCallbackRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.PaymentRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PaymentResponse;
import com.peterscode.ecommerce_management_system.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Payment Controller
 * Handles M-PESA STK Push and callbacks with enhanced security
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "APIs for payment processing")
public class PaymentController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    /**
     * Initiate M-PESA payment
     * SECURITY: Requires authentication, validates user owns order
     */
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Initiate M-PESA payment for order")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody PaymentRequest request,
            Principal principal) {

        log.info("Payment initiation request for order: {}", request.getOrderId());

        Long userId = getUserIdFromPrincipal(principal);
        PaymentResponse payment = paymentService.initiatePayment(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated. Please check your phone.", payment));
    }

    /**
     * M-PESA Callback Endpoint
     * SECURITY: IP whitelist, signature verification, idempotency
     *
     * This endpoint is called by Safaricom after user enters PIN
     * IMPORTANT: We need to read the raw request body for HMAC verification
     */
    @PostMapping("/mpesa/callback")
    @Operation(summary = "M-PESA STK Push callback (called by Safaricom)")
    public ResponseEntity<Map<String, Object>> handleMPesaCallback(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            HttpServletRequest request) {

        log.info("M-PESA callback received from IP: {}", getClientIp(request));

        try {
            // Read the raw request body for HMAC signature verification
            String rawPayload = readRawRequestBody(request);

            // Parse the JSON into our DTO
            MPesaCallbackRequest callback = objectMapper.readValue(rawPayload, MPesaCallbackRequest.class);

            String checkoutRequestID = callback.getBody().getStkCallback().getCheckoutRequestID();
            String clientIp = getClientIp(request);

            log.info("Processing callback. CheckoutRequestID: {}", checkoutRequestID);

            // Process callback asynchronously to respond quickly to Safaricom
            CompletableFuture.runAsync(() -> {
                try {
                    // Pass the raw payload for HMAC verification
                    paymentService.handleMPesaCallback(callback, rawPayload, clientIp, signature);
                } catch (Exception e) {
                    log.error("Error processing M-PESA callback: {}", checkoutRequestID, e);
                }
            });

            // Acknowledge receipt immediately (Safaricom expects quick response)
            Map<String, Object> response = new HashMap<>();
            response.put("ResultCode", 0);
            response.put("ResultDesc", "Accepted");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error handling M-PESA callback", e);

            Map<String, Object> response = new HashMap<>();
            response.put("ResultCode", 1);
            response.put("ResultDesc", "Rejected");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * M-PESA Timeout Endpoint
     * Called by Safaricom if transaction times out
     */
    @PostMapping("/mpesa/timeout")
    @Operation(summary = "M-PESA timeout callback")
    public ResponseEntity<Map<String, Object>> handleMPesaTimeout(
            @RequestBody Map<String, Object> timeoutData) {

        log.warn("M-PESA timeout received: {}", timeoutData);

        // Process timeout asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                paymentService.handleMPesaTimeout(timeoutData);
            } catch (Exception e) {
                log.error("Error processing M-PESA timeout", e);
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("ResultCode", 0);
        response.put("ResultDesc", "Accepted");

        return ResponseEntity.ok(response);
    }

    /**
     * Check payment status
     * Allows frontend to poll for payment status
     */
    @GetMapping("/status/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Check payment status for order")
    public ResponseEntity<ApiResponse<PaymentResponse>> checkPaymentStatus(
            @PathVariable Long orderId,
            Principal principal) {

        Long userId = getUserIdFromPrincipal(principal);
        PaymentResponse payment = paymentService.getPaymentStatusByOrder(orderId, userId);

        return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", payment));
    }

    /**
     * Get payment details
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get payment details")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long paymentId,
            Principal principal) {

        PaymentResponse payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", payment));
    }

    /**
     * Get all payments for user
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get user's payment history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getUserPayments(
            Principal principal) {

        Long userId = getUserIdFromPrincipal(principal);
        List<PaymentResponse> payments = paymentService.getUserPayments(userId);

        return ResponseEntity.ok(ApiResponse.success("Payments retrieved", payments));
    }

    /**
     * Admin: Get all payments
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all payments (Admin only)")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            Pageable pageable) {

        Page<PaymentResponse> payments = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved", payments));
    }

    /**
     * Admin: Process refund
     */
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process payment refund (Admin only)")
    public ResponseEntity<ApiResponse<PaymentResponse>> processRefund(
            @PathVariable Long paymentId,
            @RequestParam BigDecimal amount,
            @RequestParam String reason) {

        PaymentResponse payment = paymentService.processRefund(paymentId, amount, reason);
        return ResponseEntity.ok(ApiResponse.success("Refund processed", payment));
    }

    // --- Helper Methods ---

    /**
     * Read raw request body for HMAC signature verification
     * CRITICAL: Must preserve exact request body for signature validation
     */
    private String readRawRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        return sb.toString();
    }

    /**
     * Extract user ID from JWT principal
     */
    private Long getUserIdFromPrincipal(Principal principal) {
        // Implementation depends on your JWT configuration
        // This is a placeholder - adjust based on your security setup
        return Long.parseLong(principal.getName());
    }

    /**
     * Get client IP address
     * Handles proxy headers (X-Forwarded-For, X-Real-IP)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // If multiple IPs (proxy chain), get the first one (original client)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}