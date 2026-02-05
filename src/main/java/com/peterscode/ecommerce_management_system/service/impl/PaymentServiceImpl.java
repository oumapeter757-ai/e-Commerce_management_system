package com.peterscode.ecommerce_management_system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.peterscode.ecommerce_management_system.exception.*;

import com.peterscode.ecommerce_management_system.mapper.PaymentMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.MPesaCallbackRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.MPesaStkPushRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.PaymentRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.MPesaAuthResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.MPesaStkPushResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PaymentResponse;
import com.peterscode.ecommerce_management_system.model.entity.*;
import com.peterscode.ecommerce_management_system.model.enums.*;
import com.peterscode.ecommerce_management_system.repository.*;
import com.peterscode.ecommerce_management_system.service.NotificationService;
import com.peterscode.ecommerce_management_system.service.PaymentService;

import com.peterscode.ecommerce_management_system.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * PRODUCTION-READY M-PESA Payment Service
 *
 * Security Features:
 * - IP Whitelisting for callbacks
 * - HMAC signature verification
 * - Idempotency protection
 * - Amount validation
 * - User authorization checks
 * - Pessimistic locking for inventory
 * - Transaction isolation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ShippingRepository shippingRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final PaymentMapper paymentMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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
    @Value("${payment.min-amount:10}")
    private BigDecimal minAmount;

    @Value("${payment.max-amount:150000}")
    private BigDecimal maxAmount;

    @Value("${payment.require-full-payment:true}")
    private boolean requireFullPayment;

    // Timeout Configuration
    @Value("${payment.timeout-minutes:5}")
    private int timeoutMinutes;

    // Idempotency tracking (Note: Use Redis for distributed systems)
    private final ConcurrentHashMap<String, Boolean> processingCallbacks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> processedCallbacks = new ConcurrentHashMap<>();

    /**
     * Initiate M-PESA STK Push Payment
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse initiatePayment(PaymentRequest request, Long userId) {
        log.info("Initiating payment for order: {}, user: {}", request.getOrderId(), userId);

        try {
            // 1. SECURITY: Fetch and validate order
            Order order = validateAndFetchOrder(request.getOrderId(), userId);

            // 2. SECURITY: Validate payment amount
            validatePaymentAmount(order.getTotalAmount(), request.getAmount());

            // 3. SECURITY: Check for duplicate payment
            checkDuplicatePayment(order.getId());

            // 4. Create payment record
            Payment payment = createPaymentRecord(order);

            // 5. Get M-PESA access token
            String accessToken = getMPesaAccessToken();

            // 6. Prepare and send STK Push
            MPesaStkPushRequest stkRequest = prepareStkPushRequest(order);
            MPesaStkPushResponse stkResponse = sendStkPush(accessToken, stkRequest);

            // 7. Update payment with transaction details
            updatePaymentWithStkResponse(payment, stkResponse);

            // 8. Send notification to user
            notifyUserPaymentInitiated(order);

            log.info("STK Push successful. CheckoutRequestID: {}, Order: {}",
                    stkResponse.getCheckoutRequestID(), order.getOrderNumber());

            return paymentMapper.toResponse(payment);

        } catch (Exception e) {
            log.error("Payment initiation failed for order: {}", request.getOrderId(), e);
            handlePaymentInitiationFailure(request.getOrderId(), e);
            throw new BadRequestException("Payment initiation failed: " + e.getMessage());
        }
    }

    /**
     * Handle M-PESA callback
     * SECURITY: IP validation, HMAC verification, idempotency
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void handleMPesaCallback(MPesaCallbackRequest callbackRequest, String rawPayload, String clientIp, String signature) {
        String checkoutRequestID = callbackRequest.getBody().getStkCallback().getCheckoutRequestID();

        log.info("Received M-PESA callback. CheckoutRequestID: {}, IP: {}", checkoutRequestID, clientIp);

        try {
            // 1. SECURITY: Validate callback IP
            if (validateCallbackIp && !isValidCallbackIp(clientIp)) {
                log.error("SECURITY ALERT: Invalid callback IP: {}. Expected one of: {}",
                        clientIp, allowedIpsString);
                throw new UnauthorizedException("Invalid callback source IP");
            }

            // 2. SECURITY: Verify HMAC signature using RAW Payload
            if (verifySignature) {
                if (signature == null || signature.isEmpty()) {
                    log.error("SECURITY ALERT: Missing signature header");
                    throw new UnauthorizedException("Missing callback signature");
                }

                if (!verifyCallbackSignature(rawPayload, signature)) {
                    log.error("SECURITY ALERT: Invalid callback signature. IP: {}", clientIp);
                    throw new UnauthorizedException("Invalid callback signature");
                }

                log.debug("Callback signature verified successfully");
            }

            // 3. IDEMPOTENCY: Check if already processing
            if (processingCallbacks.putIfAbsent(checkoutRequestID, true) != null) {
                log.warn("Callback already being processed: {}", checkoutRequestID);
                return;
            }

            try {
                // 4. Find payment by transaction ID
                Payment payment = paymentRepository.findByTransactionId(checkoutRequestID)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Payment not found for CheckoutRequestID: " + checkoutRequestID));

                // 5. IDEMPOTENCY: Check if already processed
                if (isPaymentAlreadyProcessed(payment)) {
                    log.warn("Payment already processed: {}. Current status: {}",
                            checkoutRequestID, payment.getStatus());
                    return;
                }

                Order order = payment.getOrder();
                MPesaCallbackRequest.StkCallback stkCallback = callbackRequest.getBody().getStkCallback();

                // 6. Process based on result code
                if (stkCallback.getResultCode() == 0) {
                    // SUCCESS
                    processSuccessfulPayment(payment, order, stkCallback);
                } else {
                    // FAILURE
                    processFailedPayment(payment, order, stkCallback);
                }

            } finally {
                // 7. Mark as processed and remove from processing map
                processingCallbacks.remove(checkoutRequestID);
                processedCallbacks.put(checkoutRequestID, LocalDateTime.now());

                // Cleanup old processed callbacks (older than 24 hours)
                cleanupOldProcessedCallbacks();
            }

        } catch (Exception e) {
            log.error("Error processing M-PESA callback: {}", checkoutRequestID, e);
            processingCallbacks.remove(checkoutRequestID);
            throw e;
        }
    }

    /**
     * Handle M-PESA timeout callback
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void handleMPesaTimeout(Map<String, Object> timeoutData) {
        log.warn("M-PESA timeout received: {}", timeoutData);

        try {
            String checkoutRequestID = (String) timeoutData.get("CheckoutRequestID");

            if (checkoutRequestID == null) {
                log.error("Timeout callback missing CheckoutRequestID");
                return;
            }

            Payment payment = paymentRepository.findByTransactionId(checkoutRequestID)
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
            payment.setStatus(PaymentStatus.FAILED);
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

    /**
     * Get payment status by order
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatusByOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // SECURITY: Verify user owns the order
        if (!order.getUser().getId().equals(userId)) {
            log.error("SECURITY VIOLATION: User {} attempted to access payment for order {} owned by user {}",
                    userId, orderId, order.getUser().getId());
            throw new UnauthorizedException("Unauthorized access to payment");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));

        return paymentMapper.toResponse(payment);
    }

    /**
     * Get payment by ID
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        return paymentMapper.toResponse(payment);
    }

    /**
     * Get user payments
     */
    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getUserPayments(Long userId) {
        List<Payment> payments = paymentRepository.findByOrderUserId(userId);

        return payments.stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments (Admin)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        Page<Payment> payments = paymentRepository.findAll(pageable);

        return payments.map(paymentMapper::toResponse);
    }

    /**
     * Process refund (Admin)
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse processRefund(Long paymentId, BigDecimal amount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESSFUL &&
                payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException("Cannot refund payment with status: " + payment.getStatus());
        }

        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new BadRequestException("Refund amount cannot exceed payment amount");
        }

        // Update payment status
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        // Update order status
        Order order = payment.getOrder();
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        // Restore inventory
        restoreInventoryForOrder(order);

        // Notify user
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

    // --- Helper Methods ---

    private Order validateAndFetchOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // SECURITY: Verify user owns the order
        if (!order.getUser().getId().equals(userId)) {
            log.error("SECURITY VIOLATION: User {} attempted to pay for order {} owned by user {}",
                    userId, orderId, order.getUser().getId());
            throw new UnauthorizedException("Unauthorized access to order");
        }

        // Validate order status
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not in pending state: " + order.getStatus());
        }

        return order;
    }

    private void validatePaymentAmount(BigDecimal orderTotal, BigDecimal requestedAmount) {
        // SECURITY: Enforce full payment requirement
        if (requireFullPayment && requestedAmount.compareTo(orderTotal) != 0) {
            log.error("SECURITY ALERT: Payment amount mismatch. Order: {}, Requested: {}",
                    orderTotal, requestedAmount);
            throw new BadRequestException("Payment amount must match order total exactly.");
        }

        // Validate minimum amount
        if (orderTotal.compareTo(minAmount) < 0) {
            throw new BadRequestException("Amount below minimum: KES " + minAmount);
        }

        // Validate maximum amount
        if (orderTotal.compareTo(maxAmount) > 0) {
            throw new BadRequestException("Amount exceeds maximum: KES " + maxAmount);
        }
    }

    private void checkDuplicatePayment(Long orderId) {
        List<PaymentStatus> activeStatuses = Arrays.asList(
                PaymentStatus.COMPLETED,
                PaymentStatus.PROCESSING,
                PaymentStatus.SUCCESSFUL,
                PaymentStatus.PENDING
        );

        if (paymentRepository.existsByOrderIdAndStatusIn(orderId, activeStatuses)) {
            throw new BadRequestException("Active payment already exists for this order");
        }
    }

    private boolean isPaymentAlreadyProcessed(Payment payment) {
        return payment.getStatus() == PaymentStatus.COMPLETED ||
                payment.getStatus() == PaymentStatus.SUCCESSFUL ||
                payment.getStatus() == PaymentStatus.FAILED ||
                payment.getStatus() == PaymentStatus.REFUNDED;
    }

    private Payment createPaymentRecord(Order order) {
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(PaymentMethod.MPESA)
                .status(PaymentStatus.PENDING)
                .amount(order.getTotalAmount())
                .build();

        return paymentRepository.save(payment);
    }

    @Cacheable(value = "mpesaToken", unless = "#result == null")
    public String getMPesaAccessToken() {
        try {
            String auth = consumerKey + ":" + consumerSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = mpesaApiUrl + oauthEndpoint;

            log.debug("Requesting M-PESA access token from: {}", url);

            ResponseEntity<MPesaAuthResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, MPesaAuthResponse.class
            );

            if (response.getBody() == null || response.getBody().getAccessToken() == null) {
                throw new BadRequestException("Failed to get M-PESA access token");
            }

            log.debug("M-PESA access token obtained successfully");
            return response.getBody().getAccessToken();

        } catch (RestClientException e) {
            log.error("Failed to get M-PESA access token", e);
            throw new BadRequestException("M-PESA authentication failed: " + e.getMessage());
        }
    }

    private MPesaStkPushRequest prepareStkPushRequest(Order order) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());

        String password = Base64.getEncoder().encodeToString(
                (businessShortCode + passKey + timestamp).getBytes(StandardCharsets.UTF_8)
        );

        String phoneNumber = formatPhoneNumber(order.getUser().getPhoneNumber());

        return MPesaStkPushRequest.builder()
                .businessShortCode(businessShortCode)
                .password(password)
                .timestamp(timestamp)
                .transactionType("CustomerPayBillOnline")
                .amount(order.getTotalAmount().intValue())
                .partyA(phoneNumber)
                .partyB(businessShortCode)
                .phoneNumber(phoneNumber)
                .callBackURL(callbackUrl + "/mpesa/callback")
                .accountReference(order.getOrderNumber())
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
        try {
            payment.setTransactionId(response.getCheckoutRequestID());
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

        } catch (Exception e) {
            log.error("Failed to update payment record", e);
        }
    }

    private void processSuccessfulPayment(Payment payment, Order order, MPesaCallbackRequest.StkCallback callback) {
        log.info("Processing successful payment for order: {}", order.getOrderNumber());

        try {
            String mpesaReceiptNumber = extractMpesaReceiptNumber(callback);
            BigDecimal amount = extractAmount(callback);
            String phoneNumber = extractPhoneNumber(callback);

            // SECURITY: Validate amount matches
            if (amount.compareTo(order.getTotalAmount()) != 0) {
                log.error("SECURITY ALERT: Amount mismatch. Expected: {}, Received: {}",
                        order.getTotalAmount(), amount);
                throw new SecurityException("Payment amount mismatch");
            }

            // Update payment
            payment.setStatus(PaymentStatus.SUCCESSFUL);
            payment.setTransactionId(mpesaReceiptNumber);
            payment.setCreatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Update order
            order.setStatus(OrderStatus.CONFIRMED);
            order.setCreatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Deduct inventory
            deductInventoryForOrder(order);

            // Create shipping
            createShippingForOrder(order);

            // Send notifications
            notifyPaymentSuccessful(order, payment);

            log.info("Payment processed successfully. Order: {}, Receipt: {}, Amount: KES {}",
                    order.getOrderNumber(), mpesaReceiptNumber, amount);

        } catch (Exception e) {
            log.error("Error processing successful payment", e);
            throw new RuntimeException("Failed to process successful payment", e);
        }
    }

    private void processFailedPayment(Payment payment, Order order, MPesaCallbackRequest.StkCallback callback) {
        log.warn("Processing failed payment for order: {}. Reason: {}",
                order.getOrderNumber(), callback.getResultDesc());

        try {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.CANCELLED);
            order.setCancellationReason("Payment failed: " + callback.getResultDesc());
            order.setCancelledAt(LocalDateTime.now());
            orderRepository.save(order);

            releaseInventoryForOrder(order);
            notifyPaymentFailed(order, callback.getResultDesc());

        } catch (Exception e) {
            log.error("Error processing failed payment", e);
            throw new RuntimeException("Failed to process payment failure", e);
        }
    }

    /**
     * Deduct inventory after successful payment
     * Uses pessimistic locking to prevent race conditions
     */
    private void deductInventoryForOrder(Order order) {
        log.info("Deducting inventory for order: {}", order.getOrderNumber());

        for (OrderItem item : order.getOrderItems()) {
            try {
                // Fetch inventory with pessimistic write lock
                Inventory inventory = inventoryRepository.findByProductIdWithLock(item.getProduct().getId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Inventory not found for product: " + item.getProduct().getName()));

                // Deduct the quantity (includes validation)
                inventory.deductQuantity(item.getQuantity());

                // Save the updated inventory
                inventoryRepository.save(inventory);

                log.info("Deducted {} units of '{}' from inventory. Remaining: {}",
                        item.getQuantity(), item.getProduct().getName(), inventory.getAvailableStock());

                // Check if low stock and notify
                if (inventory.isLowStock()) {
                    log.warn("Low stock alert for product: {}. Available: {}",
                            item.getProduct().getName(), inventory.getAvailableStock());
                    // TODO: Send low stock notification to admins
                }

            } catch (Exception e) {
                log.error("CRITICAL: Failed to deduct inventory for product: {}",
                        item.getProduct().getName(), e);
                throw new RuntimeException("Inventory deduction failed", e);
            }
        }
    }

    /**
     * Release inventory when payment fails or order is cancelled
     */
    private void releaseInventoryForOrder(Order order) {
        log.info("Releasing inventory for order: {}", order.getOrderNumber());

        for (OrderItem item : order.getOrderItems()) {
            try {
                Inventory inventory = inventoryRepository.findByProductIdWithLock(item.getProduct().getId())
                        .orElse(null);

                if (inventory != null) {
                    inventory.releaseReservedQuantity(item.getQuantity());
                    inventoryRepository.save(inventory);

                    log.info("Released {} units of '{}' from reserved inventory",
                            item.getQuantity(), item.getProduct().getName());
                } else {
                    log.warn("Inventory not found for product: {} (may have been deleted)",
                            item.getProduct().getName());
                }

            } catch (Exception e) {
                log.error("Failed to release inventory for product: {}",
                        item.getProduct().getName(), e);
                // Don't throw - continue with other items
            }
        }
    }

    /**
     * Restore inventory after refund
     */
    private void restoreInventoryForOrder(Order order) {
        log.info("Restoring inventory for refunded order: {}", order.getOrderNumber());

        for (OrderItem item : order.getOrderItems()) {
            try {
                Inventory inventory = inventoryRepository.findByProductIdWithLock(item.getProduct().getId())
                        .orElse(null);

                if (inventory != null) {
                    inventory.addQuantity(item.getQuantity());
                    inventoryRepository.save(inventory);

                    log.info("Restored {} units of '{}' to inventory",
                            item.getQuantity(), item.getProduct().getName());
                }

            } catch (Exception e) {
                log.error("Failed to restore inventory for product: {}",
                        item.getProduct().getName(), e);
            }
        }
    }

    private void createShippingForOrder(Order order) {
        Shipping shipping = Shipping.builder()
                .order(order)
                .status(ShippingStatus.PENDING)
                .shippingAddress(order.getShippingAddress())
                .shippingCost(order.getShippingCost())
                .estimatedDeliveryDate(LocalDateTime.now().plusDays(7))
                .build();

        shippingRepository.save(shipping);
        log.info("Shipping created for order: {}", order.getOrderNumber());
    }

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

            // Compute HMAC using the RAW payload
            String computedSignature = computeHmacSignature(rawPayload, callbackSecret);

            // Compare securely using MessageDigest to prevent timing attacks
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

    private String extractMpesaReceiptNumber(MPesaCallbackRequest.StkCallback callback) {
        return extractMetadataValue(callback, "MpesaReceiptNumber");
    }

    private BigDecimal extractAmount(MPesaCallbackRequest.StkCallback callback) {
        String amountStr = extractMetadataValue(callback, "Amount");
        return amountStr != null ? new BigDecimal(amountStr) : BigDecimal.ZERO;
    }

    private String extractPhoneNumber(MPesaCallbackRequest.StkCallback callback) {
        return extractMetadataValue(callback, "PhoneNumber");
    }

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

    private void notifyUserPaymentInitiated(Order order) {
        notificationService.create(Notification.builder()
                .user(order.getUser())
                .type(NotificationType.ORDER_PLACED)
                .title("Payment Requested")
                .message("Please check your phone and enter M-PESA PIN to complete payment for order " + order.getOrderNumber())
                .referenceType("Order")
                .referenceId(order.getId())
                .priority(4)
                .build());
    }

    private void notifyPaymentSuccessful(Order order, Payment payment) {
        notificationService.create(Notification.builder()
                .user(order.getUser())
                .type(NotificationType.PAYMENT_RECEIVED)
                .title("Payment Successful")
                .message("Your payment of KES " + payment.getAmount() + " has been received. Order " + order.getOrderNumber() + " confirmed!")
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
                .message("Payment failed: " + reason + ". Please try again for order " + order.getOrderNumber())
                .referenceType("Order")
                .referenceId(order.getId())
                .priority(3)
                .build());
    }

    private void handlePaymentInitiationFailure(Long orderId, Exception e) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);

            if (order != null) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancellationReason("Payment initiation failed: " + e.getMessage());
                order.setCancelledAt(LocalDateTime.now());
                orderRepository.save(order);

                releaseInventoryForOrder(order);
            }

        } catch (Exception ex) {
            log.error("Failed to handle payment initiation failure", ex);
        }
    }

    private void cleanupOldProcessedCallbacks() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        processedCallbacks.entrySet().removeIf(entry ->
                entry.getValue().isBefore(cutoff)
        );
    }
}