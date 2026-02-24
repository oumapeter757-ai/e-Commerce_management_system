package com.peterscode.ecommerce_management_system.controller;

import tools.jackson.databind.json.JsonMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.MPesaCallbackRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.PaymentRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.MPesaQueryResponse;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.security.core.Authentication;


@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "APIs for M-PESA payment processing")
public class PaymentController {

    private final PaymentService paymentService;
    private final JsonMapper jsonMapper;

    // ==================== PAYMENT INITIATION ====================

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Initiate M-PESA STK Push payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        log.info("Payment initiation request - Order: {}, User: {}", request.getOrderId(), userId);

        PaymentResponse response = paymentService.initiatePayment(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated. Check your phone for M-PESA prompt.", response));
    }

    // ==================== M-PESA CALLBACKS ====================

    @PostMapping("/mpesa/callback")
    @Operation(summary = "M-PESA STK Push callback (called by Safaricom)", hidden = true)
    public ResponseEntity<Map<String, Object>> handleMpesaCallback(
            @RequestHeader(value = "X-Callback-Signature", required = false) String signature,
            HttpServletRequest request) {

        log.info("M-PESA callback received from IP: {}", getClientIp(request));

        try {
            // Read raw request body for HMAC signature verification
            String rawPayload = readRawRequestBody(request);
            String clientIp = getClientIp(request);

            // Parse JSON to DTO
            MPesaCallbackRequest callback = jsonMapper.readValue(rawPayload, MPesaCallbackRequest.class);

            // Process asynchronously (Safaricom expects quick response)
            CompletableFuture.runAsync(() -> {
                try {
                    paymentService.handleMPesaCallback(callback, rawPayload, clientIp, signature);
                } catch (Exception e) {
                    log.error("Error processing M-PESA callback", e);
                }
            });

            // Acknowledge immediately
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

    @PostMapping("/mpesa/timeout")
    @Operation(summary = "M-PESA timeout callback", hidden = true)
    public ResponseEntity<Map<String, Object>> handleMpesaTimeout(
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

    // ==================== PAYMENT QUERIES ====================

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get payment status for order")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(
            @PathVariable Long orderId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        PaymentResponse response = paymentService.getPaymentStatusByOrder(orderId, userId);

        return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", response));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get payment details by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable Long paymentId) {

        PaymentResponse response = paymentService.getPaymentById(paymentId);

        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", response));
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get user's payment history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getUserPayments(
            Authentication authentication) {

        Long userId = getUserId(authentication);
        List<PaymentResponse> responses = paymentService.getUserPayments(userId);

        return ResponseEntity.ok(ApiResponse.success("Payments retrieved", responses));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all payments (Admin only)")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<PaymentResponse> responses = paymentService.getAllPayments(pageable);

        return ResponseEntity.ok(ApiResponse.success("Payments retrieved", responses));
    }

    @GetMapping("/mpesa/query/{checkoutRequestId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Query M-PESA transaction status (Admin only)")
    public ResponseEntity<ApiResponse<MPesaQueryResponse>> queryMpesaTransaction(
            @PathVariable String checkoutRequestId) {

        MPesaQueryResponse response = paymentService.queryMpesaTransaction(checkoutRequestId);

        return ResponseEntity.ok(ApiResponse.success("Transaction status retrieved", response));
    }

    @GetMapping("/verify/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Verify if order is fully paid")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPayment(
            @PathVariable Long orderId) {

        boolean isVerified = paymentService.verifyPayment(orderId);
        BigDecimal totalPaid = paymentService.getTotalPaidAmount(orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("verified", isVerified);
        result.put("totalPaid", totalPaid);

        return ResponseEntity.ok(ApiResponse.success("Payment verification completed", result));
    }

    // ==================== PAYMENT MANAGEMENT ====================

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process payment refund (Admin only)")
    public ResponseEntity<ApiResponse<PaymentResponse>> processRefund(
            @PathVariable Long paymentId,
            @RequestParam BigDecimal amount,
            @RequestParam String reason) {

        PaymentResponse response = paymentService.processRefund(paymentId, amount, reason);

        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", response));
    }

    @PostMapping("/{paymentId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Cancel pending payment")
    public ResponseEntity<ApiResponse<Void>> cancelPayment(
            @PathVariable Long paymentId,
            @RequestParam String reason,
            Authentication authentication) {

        paymentService.cancelPayment(paymentId, reason);

        return ResponseEntity.ok(ApiResponse.success("Payment cancelled successfully", null));
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Read raw request body for HMAC signature verification
     * CRITICAL: Must preserve exact request body
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
     * Extract client IP from request (handles proxies)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Handle proxy chain - get original client IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid User ID in token");
        }
    }
}