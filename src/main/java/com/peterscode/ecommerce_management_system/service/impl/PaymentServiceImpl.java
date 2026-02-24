package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.*;
import com.peterscode.ecommerce_management_system.mapper.PaymentMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.MPesaCallbackRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.MPesaStkPushRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.PaymentRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.MPesaQueryResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.MPesaStkPushResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PaymentResponse;
import com.peterscode.ecommerce_management_system.model.entity.*;
import com.peterscode.ecommerce_management_system.model.enums.*;
import com.peterscode.ecommerce_management_system.repository.*;
import com.peterscode.ecommerce_management_system.service.NotificationService;
import com.peterscode.ecommerce_management_system.service.PaymentService;
import com.peterscode.ecommerce_management_system.service.EmailService;
import com.peterscode.ecommerce_management_system.service.mpesa.MpesaTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * PRODUCTION-READY PAYMENT SERVICE WITH M-PESA INTEGRATION
 *
 * Key Business Rules:
 * 1. NO PARTIAL PAYMENTS - Full order amount required before processing
 * 2. NO SHIPMENT WITHOUT PAYMENT - 100% payment confirmation required
 * 3. REAL-TIME M-PESA STK PUSH - Direct payment prompt to user's phone
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    // Repositories
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ShippingRepository shippingRepository;

    // Services
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final PaymentMapper paymentMapper;
    private final MpesaTokenService mpesaTokenService;

    // HTTP Client
    private final RestTemplate restTemplate;

    // Redis for idempotency tracking (production-safe, cluster-compatible)
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CALLBACK_PROCESSING_KEY = "payment:callback:processing:";
    private static final String CALLBACK_PROCESSED_KEY = "payment:callback:processed:";
    private static final long PROCESSING_TTL_SECONDS = 300;   // 5 minutes
    private static final long PROCESSED_TTL_HOURS = 24;       // 24 hours

    // M-PESA Configuration
    @Value("${mpesa.api.url}")
    private String mpesaApiUrl;

    @Value("${mpesa.consumer.key}")
    private String consumerKey;

    @Value("${mpesa.consumer.secret}")
    private String consumerSecret;

    @Value("${mpesa.shortcode}")
    private String businessShortCode;

    @Value("${mpesa.passkey}")
    private String passKey;

    @Value("${mpesa.callback.url}")
    private String callbackUrl;

    @Value("${mpesa.callback.secret}")
    private String callbackSecret;

    @Value("${mpesa.stk-push.endpoint}")
    private String stkPushEndpoint;

    @Value("${mpesa.query.endpoint:/mpesa/stkpushquery/v1/query}")
    private String queryEndpoint;

    @Value("${mpesa.oauth.endpoint}")
    private String oauthEndpoint;

    // Security Configuration
    @Value("${mpesa.security.allowed-ips:196.201.214.200,196.201.214.206,196.201.213.114,196.201.214.207,196.201.214.208,196.201.213.44,196.201.212.127,196.201.212.128,196.201.212.129,196.201.212.136,196.201.212.138,196.201.214.130}")
    private String allowedIpsString;

    @Value("${mpesa.security.validate-callback-ip:true}")
    private boolean validateCallbackIp;

    @Value("${mpesa.security.verify-signature:true}")
    private boolean verifySignature;

    // Payment Limits
    @Value("${payment.min-amount:1}")
    private BigDecimal minAmount;

    @Value("${payment.max-amount:150000}")
    private BigDecimal maxAmount;

    @Value("${payment.require-full-payment:true}")
    private boolean requireFullPayment;

    @Value("${payment.timeout-minutes:5}")
    private int timeoutMinutes;


    // ==================== MAIN PAYMENT METHODS ====================

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request, Long userId) {
        log.info("Initiating payment for order: {}, user: {}", request.getOrderId(), userId);

        // IDEMPOTENCY CHECK: Return existing payment if duplicate request
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingPayment.isPresent()) {
                log.info("Idempotent request detected for key: {}. Returning existing payment.", request.getIdempotencyKey());
                return paymentMapper.toResponse(existingPayment.get());
            }
        }

        Order order = null;
        Payment payment = null;

        try {
            // 1. VALIDATE ORDER & USER PERMISSIONS
            order = validateAndFetchOrder(request.getOrderId(), userId);

            // 2. VALIDATE PAYMENT AMOUNT (FULL PAYMENT REQUIRED)
            validatePaymentAmount(order.getTotalAmount(), request.getAmount());

            // 3. CHECK FOR DUPLICATE ACTIVE PAYMENTS
            checkDuplicatePayment(order.getId());

            // 4. RESERVE INVENTORY
            reserveInventoryForOrder(order);

            // 5. CREATE PAYMENT RECORD
            payment = createPaymentRecord(order, request.getPhoneNumber());
            if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
                payment.setIdempotencyKey(request.getIdempotencyKey());
                payment = paymentRepository.save(payment);
            }

            // 6. INITIATE M-PESA STK PUSH
            String accessToken = mpesaTokenService.getAccessToken();
            MPesaStkPushRequest stkRequest = prepareStkPushRequest(order, payment.getId());
            MPesaStkPushResponse stkResponse = sendStkPush(accessToken, stkRequest);

            // 7. UPDATE PAYMENT WITH STK RESPONSE
            updatePaymentWithStkResponse(payment, stkResponse);

            // 8. SEND NOTIFICATIONS
            notifyUserPaymentInitiated(order, payment);

            log.info("✅ STK Push initiated. CheckoutRequestID: {}, Order: {}",
                    stkResponse.getCheckoutRequestID(), order.getOrderNumber());

            return paymentMapper.toResponse(payment);

        } catch (Exception e) {
            log.error("❌ Payment initiation failed for order: {}", request.getOrderId(), e);

            // ROLLBACK: Release inventory on failure
            if (order != null) {
                releaseInventoryForOrder(order);
            }

            // Cancel payment record if created
            if (payment != null) {
                payment.markAsFailed("Payment initiation failed: " + e.getMessage());
                paymentRepository.save(payment);
            }

            throw new BadRequestException("Payment initiation failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleMPesaCallback(MPesaCallbackRequest callbackRequest,
                                    String rawPayload,
                                    String clientIp,
                                    String signature) {
        String checkoutRequestID = callbackRequest.getBody().getStkCallback().getCheckoutRequestID();
        String merchantRequestID = callbackRequest.getBody().getStkCallback().getMerchantRequestID();

        log.info("Received M-PESA callback. CheckoutRequestID: {}, MerchantRequestID: {}, IP: {}",
                checkoutRequestID, merchantRequestID, clientIp);

        try {
            // 1. SECURITY: VALIDATE CALLBACK IP
            if (validateCallbackIp && !isValidCallbackIp(clientIp)) {
                log.error("🚨 SECURITY ALERT: Invalid callback IP: {}. Expected: {}", clientIp, allowedIpsString);
                throw new UnauthorizedException("Invalid callback source IP");
            }

            // 2. SECURITY: VERIFY HMAC SIGNATURE
            if (verifySignature && !verifyCallbackSignature(rawPayload, signature)) {
                log.error("🚨 SECURITY ALERT: Invalid callback signature. IP: {}", clientIp);
                throw new UnauthorizedException("Invalid callback signature");
            }

            // 3. IDEMPOTENCY: CHECK IF ALREADY PROCESSING (Redis-backed, cluster-safe)
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(CALLBACK_PROCESSING_KEY + checkoutRequestID, "1",
                            PROCESSING_TTL_SECONDS, TimeUnit.SECONDS);

            if (!Boolean.TRUE.equals(acquired)) {
                log.warn("Callback already being processed: {}", checkoutRequestID);
                return;
            }

            try {
                // Check if already processed in last 24 hours
                if (Boolean.TRUE.equals(redisTemplate.hasKey(CALLBACK_PROCESSED_KEY + checkoutRequestID))) {
                    log.warn("Callback already processed: {}", checkoutRequestID);
                    return;
                }

                // 4. FIND PAYMENT
                Payment payment = paymentRepository.findByCheckoutRequestId(checkoutRequestID)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Payment not found for CheckoutRequestID: " + checkoutRequestID));

                // 5. IDEMPOTENCY: CHECK IF ALREADY PROCESSED
                if (isPaymentAlreadyProcessed(payment)) {
                    log.warn("Payment already processed: {}. Status: {}", checkoutRequestID, payment.getStatus());
                    return;
                }

                Order order = payment.getOrder();
                MPesaCallbackRequest.StkCallback stkCallback = callbackRequest.getBody().getStkCallback();

                // 6. PROCESS BASED ON RESULT CODE
                if (stkCallback.getResultCode() == 0) {
                    processSuccessfulPayment(payment, order, stkCallback);
                } else {
                    processFailedPayment(payment, order, stkCallback);
                }

            } finally {
                // 7. CLEANUP: Remove processing flag, mark as processed
                redisTemplate.delete(CALLBACK_PROCESSING_KEY + checkoutRequestID);
                redisTemplate.opsForValue().set(
                        CALLBACK_PROCESSED_KEY + checkoutRequestID, "1",
                        PROCESSED_TTL_HOURS, TimeUnit.HOURS);
            }

        } catch (Exception e) {
            log.error("Error processing M-PESA callback: {}", checkoutRequestID, e);
            redisTemplate.delete(CALLBACK_PROCESSING_KEY + checkoutRequestID);
            throw e;
        }
    }

    @Override
    @Transactional
    public void handleMPesaTimeout(Map<String, Object> timeoutData) {
        log.warn("M-PESA timeout received: {}", timeoutData);

        try {
            String checkoutRequestID = (String) timeoutData.get("CheckoutRequestID");

            if (checkoutRequestID == null) {
                log.error("Timeout callback missing CheckoutRequestID");
                return;
            }

            Payment payment = paymentRepository.findByCheckoutRequestId(checkoutRequestID)
                    .orElse(null);

            if (payment == null) {
                log.warn("Payment not found for timeout callback: {}", checkoutRequestID);
                return;
            }

            if (isPaymentAlreadyProcessed(payment)) {
                log.info("Payment already processed, ignoring timeout: {}", checkoutRequestID);
                return;
            }

            // Mark payment as failed due to timeout
            payment.markAsFailed("Payment timeout - user did not respond");
            paymentRepository.save(payment);

            // Cancel order
            Order order = payment.getOrder();
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancellationReason("Payment timeout - user did not respond");
            order.setCancelledAt(LocalDateTime.now());
            orderRepository.save(order);

            // Release inventory
            releaseInventoryForOrder(order);

            // Notify user
            notifyPaymentFailed(order, "Payment request timed out. Please try again.");

            log.info("Payment timeout processed for order: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Error processing M-PESA timeout", e);
        }
    }

    @Override
    public MPesaQueryResponse queryMpesaTransaction(String checkoutRequestId) {
        try {
            String accessToken = mpesaTokenService.getAccessToken();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            String password = Base64.getEncoder().encodeToString(
                    (businessShortCode + passKey + timestamp).getBytes(StandardCharsets.UTF_8)
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("BusinessShortCode", businessShortCode);
            requestBody.put("Password", password);
            requestBody.put("Timestamp", timestamp);
            requestBody.put("CheckoutRequestID", checkoutRequestId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = mpesaApiUrl + queryEndpoint;

            ResponseEntity<MPesaQueryResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, MPesaQueryResponse.class);

            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to query M-Pesa transaction: {}", checkoutRequestId, e);
            throw new BadRequestException("Failed to query transaction status: " + e.getMessage());
        }
    }

    // ==================== PAYMENT MANAGEMENT ====================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatusByOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // SECURITY: Verify user owns the order
        if (!order.getUser().getId().equals(userId)) {
            log.error("🚨 SECURITY VIOLATION: User {} attempted to access payment for order {} owned by user {}",
                    userId, orderId, order.getUser().getId());
            throw new UnauthorizedException("Unauthorized access to payment");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getUserPayments(Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);

        return payments.stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        Page<Payment> payments = paymentRepository.findAll(pageable);

        return payments.map(paymentMapper::toResponse);
    }

    @Override
    @Transactional
    public PaymentResponse processRefund(Long paymentId, BigDecimal amount, String reason) {
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        // Validate refund eligibility
        if (payment.getStatus() != PaymentStatus.SUCCESSFUL) {
            throw new BadRequestException("Cannot refund payment with status: " + payment.getStatus());
        }

        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new BadRequestException("Refund amount cannot exceed payment amount");
        }

        // Update payment
        payment.markAsRefunded();
        paymentRepository.save(payment);

        // Update order
        Order order = payment.getOrder();
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        // Restore inventory
        restoreInventoryForOrder(order);

        // Send notifications
        notificationService.create(Notification.builder()
                .user(order.getUser())
                .type(NotificationType.PAYMENT_REFUNDED)
                .title("Refund Processed")
                .message("Your payment of KES " + amount + " has been refunded. Reason: " + reason)
                .referenceType("Order")
                .referenceId(order.getId())
                .priority(3)
                .build());

        log.info("Refund processed for payment: {}, amount: {}, reason: {}", paymentId, amount, reason);

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public void cancelPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        // Only cancel pending or processing payments
        if (payment.getStatus() != PaymentStatus.PENDING &&
                payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new BadRequestException("Cannot cancel payment with status: " + payment.getStatus());
        }

        payment.markAsCancelled(reason);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);

        releaseInventoryForOrder(order);

        log.info("Payment cancelled: {}, reason: {}", paymentId, reason);
    }

    @Override
    public boolean verifyPayment(Long orderId) {
        BigDecimal totalPaid = getTotalPaidAmount(orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // Payment is verified if 100% of order amount has been paid
        return totalPaid.compareTo(order.getTotalAmount()) >= 0;
    }

    @Override
    public BigDecimal getTotalPaidAmount(Long orderId) {
        return paymentRepository.getTotalPaidAmountForOrder(orderId);
    }

    // ==================== SCHEDULED TASKS ====================

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void cleanupTimedOutPayments() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Payment> timedOutPayments = paymentRepository.findProcessingPaymentsOlderThan(timeoutThreshold);

        for (Payment payment : timedOutPayments) {
            log.warn("Processing timed out payment: {}", payment.getCheckoutRequestId());
            handleMPesaTimeout(Collections.singletonMap(
                    "CheckoutRequestID", payment.getCheckoutRequestId()
            ));
        }
    }

    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    @Transactional
    public void auditPendingPayments() {
        // Redis TTL handles callback cleanup automatically
        // This task audits any orphaned processing payments
        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        List<Payment> stalePayments = paymentRepository.findProcessingPaymentsOlderThan(threshold);
        if (!stalePayments.isEmpty()) {
            log.warn("Found {} stale processing payments older than 2 hours", stalePayments.size());
        }
    }

    // ==================== HELPER METHODS ====================

    private Order validateAndFetchOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // SECURITY: Verify user owns the order
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Unauthorized access to order");
        }

        // Validate order status
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Order cannot be paid. Current status: " + order.getStatus());
        }

        // Check if order already fully paid
        if (verifyPayment(orderId)) {
            throw new BadRequestException("Order has already been fully paid");
        }

        return order;
    }

    private void validatePaymentAmount(BigDecimal orderTotal, BigDecimal requestedAmount) {
        // CRITICAL: Enforce full payment requirement
        if (requireFullPayment && requestedAmount.compareTo(orderTotal) != 0) {
            log.error("🚨 PAYMENT POLICY VIOLATION: Payment amount mismatch. Order: {}, Requested: {}",
                    orderTotal, requestedAmount);
            throw new BadRequestException("Full payment required. Amount must match order total exactly: KES " + orderTotal);
        }

        // Validate against M-PESA limits
        if (orderTotal.compareTo(minAmount) < 0) {
            throw new BadRequestException("Amount below M-PESA minimum: KES " + minAmount);
        }

        if (orderTotal.compareTo(maxAmount) > 0) {
            throw new BadRequestException("Amount exceeds M-PESA maximum: KES " + maxAmount);
        }
    }

    private void checkDuplicatePayment(Long orderId) {
        List<PaymentStatus> activeStatuses = Arrays.asList(
                PaymentStatus.PROCESSING,
                PaymentStatus.PENDING
        );

        if (paymentRepository.existsByOrderIdAndStatusIn(orderId, activeStatuses)) {
            throw new BadRequestException("Active payment already exists for this order");
        }
    }

    private Payment createPaymentRecord(Order order, String phoneNumber) {
        Payment payment = Payment.builder()
                .order(order)
                .user(order.getUser())
                .paymentMethod(PaymentMethod.MPESA)
                .status(PaymentStatus.PENDING)
                .amount(order.getTotalAmount())
                .paymentDetails("Phone: " + formatPhoneNumber(phoneNumber))
                .build();

        return paymentRepository.save(payment);
    }

    private MPesaStkPushRequest prepareStkPushRequest(Order order, Long paymentId) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());

        String password = Base64.getEncoder().encodeToString(
                (businessShortCode + passKey + timestamp).getBytes(StandardCharsets.UTF_8)
        );

        String phoneNumber = formatPhoneNumber(order.getUser().getPhoneNumber());

        // Create unique reference combining order number and payment ID
        String accountReference = order.getOrderNumber() + "_PAY" + paymentId;

        return MPesaStkPushRequest.builder()
                .businessShortCode(businessShortCode)
                .password(password)
                .timestamp(timestamp)
                .transactionType("CustomerPayBillOnline")
                .amount(order.getTotalAmount().intValue())
                .partyA(phoneNumber)
                .partyB(businessShortCode)
                .phoneNumber(phoneNumber)
                .callBackURL(callbackUrl + "/api/v1/payments/mpesa/callback")
                .accountReference(accountReference)
                .transactionDesc("Payment for Order " + order.getOrderNumber())
                .build();
    }

    private MPesaStkPushResponse sendStkPush(String accessToken, MPesaStkPushRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MPesaStkPushRequest> entity = new HttpEntity<>(request, headers);
            String url = mpesaApiUrl + stkPushEndpoint;

            log.debug("Sending STK Push to: {}", url);

            ResponseEntity<MPesaStkPushResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, MPesaStkPushResponse.class
            );

            if (response.getBody() == null) {
                throw new BadRequestException("Empty response from M-PESA");
            }

            if (!"0".equals(response.getBody().getResponseCode())) {
                throw new BadRequestException(
                        "M-PESA error: " + response.getBody().getResponseDescription());
            }

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Failed to send STK Push", e);
            throw new BadRequestException("Failed to initiate M-PESA payment: " + e.getMessage());
        }
    }

    private void updatePaymentWithStkResponse(Payment payment, MPesaStkPushResponse response) {
        payment.markAsProcessing(response.getCheckoutRequestID(), response.getMerchantRequestID());
        paymentRepository.save(payment);
    }

    private void processSuccessfulPayment(Payment payment, Order order, MPesaCallbackRequest.StkCallback callback) {
        log.info("💰 Processing successful payment for order: {}", order.getOrderNumber());

        try {
            // Extract payment details from callback
            String mpesaReceiptNumber = extractMetadataValue(callback, "MpesaReceiptNumber");
            BigDecimal amount = extractAmount(callback);
            String phoneNumber = extractMetadataValue(callback, "PhoneNumber");

            // CRITICAL: Validate amount matches exactly
            if (amount.compareTo(order.getTotalAmount()) != 0) {
                log.error("🚨 PAYMENT AMOUNT MISMATCH: Expected: {}, Received: {}",
                        order.getTotalAmount(), amount);
                throw new SecurityException("Payment amount mismatch");
            }

            // Update payment record using entity method
            String callbackMeta = "Receipt: " + mpesaReceiptNumber + ", Phone: " + phoneNumber;
            payment.markAsSuccessful(mpesaReceiptNumber, phoneNumber, callbackMeta);
            paymentRepository.save(payment);

            // Update order (CRITICAL: Only after full payment)
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaidAt(LocalDateTime.now());
            orderRepository.save(order);

            // CRITICAL: Deduct inventory (Now that payment is 100% confirmed)
            deductInventoryForOrder(order);

            // Create shipping record (Now that order is fully paid)
            createShippingForOrder(order);

            // Send success notifications
            notifyPaymentSuccessful(order, payment);
            sendPaymentSuccessEmail(order, payment);

            log.info("✅ Payment processed successfully. Order: {}, Receipt: {}, Amount: KES {}",
                    order.getOrderNumber(), mpesaReceiptNumber, amount);

        } catch (Exception e) {
            log.error("Error processing successful payment", e);
            // On error, mark payment as failed to prevent partial processing
            payment.markAsFailed("Processing error: " + e.getMessage());
            paymentRepository.save(payment);
            throw new RuntimeException("Failed to process successful payment", e);
        }
    }

    private void processFailedPayment(Payment payment, Order order, MPesaCallbackRequest.StkCallback callback) {
        log.warn("❌ Processing failed payment for order: {}. Reason: {}",
                order.getOrderNumber(), callback.getResultDesc());

        try {
            payment.markAsFailed(callback.getResultDesc());
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.CANCELLED);
            order.setCancellationReason("Payment failed: " + callback.getResultDesc());
            order.setCancelledAt(LocalDateTime.now());
            orderRepository.save(order);

            // CRITICAL: Release reserved inventory
            releaseInventoryForOrder(order);

            notifyPaymentFailed(order, callback.getResultDesc());
            sendPaymentFailureEmail(order, callback.getResultDesc());

        } catch (Exception e) {
            log.error("Error processing failed payment", e);
            throw new RuntimeException("Failed to process payment failure", e);
        }
    }

    // ==================== INVENTORY MANAGEMENT ====================

    private void reserveInventoryForOrder(Order order) {
        log.info("Reserving inventory for order: {}", order.getOrderNumber());

        for (OrderItem item : order.getOrderItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for product: " + item.getProduct().getName()));

            // Reserve the quantity (deduct from available, add to reserved)
            inventory.reserveQuantity(item.getQuantity());
            inventoryRepository.save(inventory);

            log.debug("Reserved {} units of '{}'. Available: {}, Reserved: {}",
                    item.getQuantity(), item.getProduct().getName(),
                    inventory.getAvailableStock(), inventory.getReservedStock());
        }
    }

    private void deductInventoryForOrder(Order order) {
        log.info("Deducting inventory for paid order: {}", order.getOrderNumber());

        for (OrderItem item : order.getOrderItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for product: " + item.getProduct().getName()));

            // Final deduction from reserved stock
            inventory.confirmReservedQuantity(item.getQuantity());
            inventoryRepository.save(inventory);

            log.info("Deducted {} units of '{}' from reserved inventory. Remaining: {}",
                    item.getQuantity(), item.getProduct().getName(), inventory.getAvailableStock());

            // Low stock notification
            if (inventory.isLowStock()) {
                log.warn("⚠️ Low stock alert for product: {}. Available: {}",
                        item.getProduct().getName(), inventory.getAvailableStock());

                notificationService.create(Notification.builder()
                        .user(order.getUser())
                        .type(NotificationType.LOW_STOCK)
                        .title("Low Stock Alert")
                        .message("Product '" + item.getProduct().getName() + "' is running low")
                        .referenceType("Product")
                        .referenceId(item.getProduct().getId())
                        .priority(3)
                        .build());
            }
        }
    }

    private void releaseInventoryForOrder(Order order) {
        log.info("Releasing inventory for cancelled order: {}", order.getOrderNumber());

        for (OrderItem item : order.getOrderItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                    .orElse(null);

            if (inventory != null) {
                inventory.releaseReservedQuantity(item.getQuantity());
                inventoryRepository.save(inventory);

                log.info("Released {} units of '{}' back to available stock",
                        item.getQuantity(), item.getProduct().getName());
            }
        }
    }

    private void restoreInventoryForOrder(Order order) {
        log.info("Restoring inventory for refunded order: {}", order.getOrderNumber());

        for (OrderItem item : order.getOrderItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                    .orElse(null);

            if (inventory != null) {
                inventory.addQuantity(item.getQuantity());
                inventoryRepository.save(inventory);

                log.info("Restored {} units of '{}' to inventory",
                        item.getQuantity(), item.getProduct().getName());
            }
        }
    }

    private void createShippingForOrder(Order order) {
        Shipping shipping = Shipping.builder()
                .order(order)
                .status(ShippingStatus.PENDING)
                .carrier("PENDING_ASSIGNMENT")
                .shippingMethod("Standard")
                .shippingAddress(order.getShippingAddress())
                .shippingCost(order.getShippingCost() != null ? order.getShippingCost() : BigDecimal.ZERO)
                .estimatedDeliveryDate(LocalDateTime.now().plusDays(7))
                .build();

        shippingRepository.save(shipping);
        log.info("Shipping created for paid order: {}", order.getOrderNumber());
    }

    // ==================== SECURITY & VALIDATION ====================

    private boolean isValidCallbackIp(String clientIp) {
        if (clientIp == null || clientIp.isEmpty()) {
            return false;
        }

        String[] allowedIps = allowedIpsString.split(",");

        for (String allowedIp : allowedIps) {
            if (clientIp.trim().equals(allowedIp.trim())) {
                return true;
            }
        }

        return false;
    }

    private boolean verifyCallbackSignature(String rawPayload, String signature) {
        try {
            if (rawPayload == null || signature == null) {
                return false;
            }

            String computedSignature = computeHmacSignature(rawPayload, callbackSecret);

            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    computedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            log.error("Failed to verify signature", e);
            return false;
        }
    }

    private String computeHmacSignature(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            throw new BadRequestException("Phone number is required");
        }

        String cleaned = phone.replaceAll("[^0-9]", "");

        // Kenyan phone number format
        if (cleaned.startsWith("0")) {
            return "254" + cleaned.substring(1);
        }
        if (cleaned.startsWith("254")) {
            return cleaned;
        }
        if (cleaned.startsWith("7") || cleaned.startsWith("1")) {
            return "254" + cleaned;
        }
        if (cleaned.startsWith("+254")) {
            return cleaned.substring(1);
        }

        throw new BadRequestException("Invalid Kenyan phone number format");
    }

    private boolean isPaymentAlreadyProcessed(Payment payment) {
        return payment.getStatus() == PaymentStatus.SUCCESSFUL ||
                payment.getStatus() == PaymentStatus.FAILED ||
                payment.getStatus() == PaymentStatus.REFUNDED ||
                payment.getStatus() == PaymentStatus.CANCELLED;
    }

    // ==================== NOTIFICATION METHODS ====================

    private void notifyUserPaymentInitiated(Order order, Payment payment) {
        notificationService.create(Notification.builder()
                .user(order.getUser())
                .type(NotificationType.PAYMENT_INITIATED)
                .title("Payment Requested")
                .message("Please check your phone and enter M-PESA PIN to pay KES " +
                        payment.getAmount() + " for order " + order.getOrderNumber())
                .referenceType("Order")
                .referenceId(order.getId())
                .priority(4)
                .build());
    }

    private void notifyPaymentSuccessful(Order order, Payment payment) {
        notificationService.create(Notification.builder()
                .user(order.getUser())
                .type(NotificationType.PAYMENT_SUCCESSFUL)
                .title("Payment Successful")
                .message("Your payment of KES " + payment.getAmount() +
                        " has been received. Order " + order.getOrderNumber() + " confirmed!")
                .referenceType("Order")
                .referenceId(order.getId())
                .priority(3)
                .build());
    }

    private void notifyPaymentFailed(Order order, String reason) {
        notificationService.create(Notification.builder()
                .user(order.getUser())
                .type(NotificationType.PAYMENT_FAILED)
                .title("Payment Failed")
                .message("Payment failed: " + reason +
                        ". Please try again for order " + order.getOrderNumber())
                .referenceType("Order")
                .referenceId(order.getId())
                .priority(3)
                .build());
    }

    private void sendPaymentSuccessEmail(Order order, Payment payment) {
        try {
            String subject = "Payment Successful - Order #" + order.getOrderNumber();
            String body = buildPaymentSuccessEmail(
                    order.getUser().getFirstName(),
                    order.getOrderNumber(),
                    payment.getAmount(),
                    payment.getMpesaReceiptNumber()
            );

            emailService.sendHtmlEmail(order.getUser().getEmail(), subject, body);

        } catch (Exception e) {
            log.error("Failed to send payment success email", e);
        }
    }

    private void sendPaymentFailureEmail(Order order, String reason) {
        try {
            String subject = "Payment Failed - Order #" + order.getOrderNumber();
            String body = buildPaymentFailureEmail(
                    order.getUser().getFirstName(),
                    order.getOrderNumber(),
                    reason
            );

            emailService.sendHtmlEmail(order.getUser().getEmail(), subject, body);

        } catch (Exception e) {
            log.error("Failed to send payment failure email", e);
        }
    }

    // ==================== UTILITY METHODS ====================

    private String extractMetadataValue(MPesaCallbackRequest.StkCallback callback, String name) {
        if (callback.getCallbackMetadata() == null || callback.getCallbackMetadata().getItem() == null) {
            return null;
        }

        return callback.getCallbackMetadata().getItem().stream()
                .filter(item -> name.equals(item.getName()))
                .map(item -> String.valueOf(item.getValue()))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal extractAmount(MPesaCallbackRequest.StkCallback callback) {
        String amountStr = extractMetadataValue(callback, "Amount");
        return amountStr != null ? new BigDecimal(amountStr) : BigDecimal.ZERO;
    }


    // ==================== EMAIL TEMPLATES ====================

    private String buildPaymentSuccessEmail(String username, String orderNumber,
                                            BigDecimal amount, String receiptNumber) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background: #f9f9f9; }
                    .details { background: white; padding: 20px; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; padding: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Payment Successful!</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s,</h2>
                        <p>Your payment has been successfully processed and your order is now confirmed.</p>
                        
                        <div class="details">
                            <h3>Payment Details:</h3>
                            <p><strong>Order Number:</strong> %s</p>
                            <p><strong>Amount Paid:</strong> KES %s</p>
                            <p><strong>M-Pesa Receipt:</strong> %s</p>
                            <p><strong>Date & Time:</strong> %s</p>
                        </div>
                        
                        <p>Your order is now being processed and will be shipped soon. 
                           You'll receive another notification when your order ships.</p>
                        
                        <p>Thank you for shopping with us!</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 E-commerce Platform. All rights reserved.</p>
                        <p>This is an automated message, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, orderNumber, amount, receiptNumber, currentDate);
    }

    private String buildPaymentFailureEmail(String username, String orderNumber, String reason) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #f44336; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background: #f9f9f9; }
                    .details { background: white; padding: 20px; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; padding: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>❌ Payment Failed</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s,</h2>
                        <p>We were unable to process your payment for the following order:</p>
                        
                        <div class="details">
                            <h3>Order Details:</h3>
                            <p><strong>Order Number:</strong> %s</p>
                            <p><strong>Reason for Failure:</strong> %s</p>
                        </div>
                        
                        <p>Please try the payment again or contact our support team if the issue persists.</p>
                        <p>Your order has been temporarily reserved, but will be released if payment is not completed within 24 hours.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 E-commerce Platform. All rights reserved.</p>
                        <p>This is an automated message, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, orderNumber, reason);
    }
}

