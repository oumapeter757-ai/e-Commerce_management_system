package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.*;
import com.peterscode.ecommerce_management_system.mapper.OrderMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.CheckoutRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.OrderRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.OrderResponse;
import com.peterscode.ecommerce_management_system.model.entity.*;
import com.peterscode.ecommerce_management_system.model.enums.NotificationType;
import com.peterscode.ecommerce_management_system.model.enums.OrderStatus;
import com.peterscode.ecommerce_management_system.repository.*;
import com.peterscode.ecommerce_management_system.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final NotificationService notificationService;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, Long userId) {
        log.info("Creating order for user: {}", userId);

        // 1. Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Validate addresses belong to user
        Address shippingAddress = addressRepository.findById(request.getShippingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));

        Address billingAddress = addressRepository.findById(request.getBillingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Billing address not found"));

        if (!shippingAddress.getUser().getId().equals(userId) || !billingAddress.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Address does not belong to user");
        }

        // 3. Create order with PENDING status
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .couponCode(request.getCouponCode())
                .customerNotes(request.getCustomerNotes())
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingCost(BigDecimal.ZERO)
                .build();

        // 4. Process each order item
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.getProductId()));

            // Check stock availability BEFORE reserving
            if (!inventoryService.isStockAvailable(product.getId(), itemRequest.getQuantity())) {
                throw new InsufficientStockException(
                        String.format("Insufficient stock for product: %s. Requested: %d",
                                product.getName(), itemRequest.getQuantity()));
            }

            // Reserve stock (business logic in service layer)
            inventoryService.reserveStock(product.getId(), itemRequest.getQuantity());

            // Update product sold count
            productRepository.incrementSoldCount(product.getId(), itemRequest.getQuantity());

            // Create order item
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .productImageUrl(product.getImageUrl())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getActualPrice())
                    .build();

            orderItem.calculateTotalPrice();
            order.addOrderItem(orderItem);
        }

        // 5. Calculate order totals
        order.calculateTotals();

        // 6. Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Order created: {} for user: {}", savedOrder.getOrderNumber(), userId);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse checkoutFromCart(CheckoutRequest request, Long userId) {
        log.info("Kilimall-style checkout for user: {}", userId);

        // 1. Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Validate addresses belong to user (IDOR prevention)
        Address shippingAddress = addressRepository.findById(request.getShippingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));
        Address billingAddress = addressRepository.findById(request.getBillingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Billing address not found"));

        if (!shippingAddress.getUser().getId().equals(userId) || !billingAddress.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Address does not belong to user");
        }

        // 3. Fetch cart with selected items only
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BadRequestException("Cart not found"));

        List<CartItem> selectedItems = cart.getItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.getSelected()))
                .filter(item -> !Boolean.TRUE.equals(item.getSavedForLater()))
                .toList();

        if (selectedItems.isEmpty()) {
            throw new BadRequestException("No items selected for checkout");
        }

        // 4. Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .couponCode(request.getCouponCode())
                .customerNotes(request.getCustomerNotes())
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingCost(cart.getEstimatedShippingCost() != null ? cart.getEstimatedShippingCost() : BigDecimal.ZERO)
                .build();

        // 5. Process each selected cart item → order item
        for (CartItem cartItem : selectedItems) {
            Product product = cartItem.getProduct();

            if (!Boolean.TRUE.equals(product.getIsActive())) {
                throw new BadRequestException("Product is no longer available: " + product.getName());
            }

            if (!inventoryService.isStockAvailable(product.getId(), cartItem.getQuantity())) {
                throw new InsufficientStockException(
                        String.format("Insufficient stock for %s. Requested: %d",
                                product.getName(), cartItem.getQuantity()));
            }

            // Reserve stock
            inventoryService.reserveStock(product.getId(), cartItem.getQuantity());
            productRepository.incrementSoldCount(product.getId(), cartItem.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .productImageUrl(product.getImageUrl())
                    .productVariant(cartItem.getProductVariant())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getActualPrice())
                    .discountPrice(product.getDiscountPrice())
                    .build();

            orderItem.calculateTotalPrice();
            order.addOrderItem(orderItem);
        }

        // 6. Calculate totals
        order.calculateTotals();

        // 7. Apply coupon if provided
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            BigDecimal discount = couponService.validateAndApplyCoupon(
                    request.getCouponCode(), order.getSubtotal(), userId);
            order.setDiscountAmount(discount);
            order.calculateTotals();
        }

        // 8. Save order
        Order savedOrder = orderRepository.save(order);

        // 9. Record coupon usage
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            couponService.recordCouponUsage(request.getCouponCode(), userId, savedOrder.getId());
        }

        // 10. Remove checked-out items from cart
        selectedItems.forEach(item -> {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        });
        cart.recalculateTotals();
        cartRepository.save(cart);

        // 11. Send order placed notification
        sendOrderNotification(savedOrder, NotificationType.ORDER_PLACED);

        log.info("Checkout order created: {} for user: {} with {} items",
                savedOrder.getOrderNumber(), userId, selectedItems.size());

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus oldStatus = order.getStatus();
        log.info("Updating order {} status from {} to {}", orderId, oldStatus, newStatus);

        // 1. Handle state transitions with inventory logic
        switch (newStatus) {
            case CONFIRMED:
                // Payment successful - confirm reserved stock
                if (oldStatus == OrderStatus.PENDING) {
                    confirmOrderStock(order);
                    order.setPaidAt(LocalDateTime.now());
                }
                break;

            case CANCELLED:
                // Order cancelled - release reserved stock
                if (oldStatus == OrderStatus.PENDING) {
                    releaseOrderReservation(order);
                } else if (oldStatus == OrderStatus.CONFIRMED || oldStatus == OrderStatus.PROCESSING) {
                    // Already confirmed - need to restock
                    restockReturnedItems(order);
                }
                order.setCancelledAt(LocalDateTime.now());
                break;

            case RETURNED:
                // Items returned - restock inventory
                restockReturnedItems(order);
                break;

            case FAILED:
                // Payment failed - release reservation
                if (oldStatus == OrderStatus.PENDING) {
                    releaseOrderReservation(order);
                }
                break;

            case DELIVERED:
                order.setDeliveredAt(LocalDateTime.now());
                break;
        }

        // 2. Update order status
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        // 3. Send status change notification to user
        sendOrderNotification(savedOrder, mapOrderStatusToNotificationType(newStatus));

        log.info("Order {} status updated to {}", orderId, newStatus);
        return orderMapper.toResponse(savedOrder);
    }

    // ========== NOTIFICATION HELPER ==========

    private void sendOrderNotification(Order order, NotificationType type) {
        try {
            Notification notification = Notification.builder()
                    .user(order.getUser())
                    .type(type)
                    .title(getNotificationTitle(type, order.getOrderNumber()))
                    .message(getNotificationMessage(type, order.getOrderNumber()))
                    .isRead(false)
                    .build();
            notificationService.create(notification);
        } catch (Exception e) {
            log.warn("Failed to send order notification for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    private NotificationType mapOrderStatusToNotificationType(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> NotificationType.ORDER_CONFIRMED;
            case SHIPPED -> NotificationType.ORDER_SHIPPED;
            case DELIVERED -> NotificationType.ORDER_DELIVERED;
            case CANCELLED -> NotificationType.ORDER_CANCELLED;
            case REFUNDED -> NotificationType.ORDER_REFUNDED;
            default -> NotificationType.ORDER_PLACED;
        };
    }

    private String getNotificationTitle(NotificationType type, String orderNumber) {
        return switch (type) {
            case ORDER_PLACED -> "Order Placed - " + orderNumber;
            case ORDER_CONFIRMED -> "Order Confirmed - " + orderNumber;
            case ORDER_SHIPPED -> "Order Shipped - " + orderNumber;
            case ORDER_DELIVERED -> "Order Delivered - " + orderNumber;
            case ORDER_CANCELLED -> "Order Cancelled - " + orderNumber;
            case ORDER_REFUNDED -> "Order Refunded - " + orderNumber;
            default -> "Order Update - " + orderNumber;
        };
    }

    private String getNotificationMessage(NotificationType type, String orderNumber) {
        return switch (type) {
            case ORDER_PLACED -> "Your order " + orderNumber + " has been placed successfully.";
            case ORDER_CONFIRMED -> "Your order " + orderNumber + " has been confirmed and is being processed.";
            case ORDER_SHIPPED -> "Your order " + orderNumber + " has been shipped!";
            case ORDER_DELIVERED -> "Your order " + orderNumber + " has been delivered.";
            case ORDER_CANCELLED -> "Your order " + orderNumber + " has been cancelled.";
            case ORDER_REFUNDED -> "Your order " + orderNumber + " has been refunded.";
            default -> "Order " + orderNumber + " has been updated.";
        };
    }

    // ========== INVENTORY HELPER METHODS ==========

    private void confirmOrderStock(Order order) {
        log.info("Confirming stock reduction for order: {}", order.getOrderNumber());
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.confirmStockReduction(item.getProduct().getId(), item.getQuantity());
        }
    }

    private void releaseOrderReservation(Order order) {
        log.info("Releasing reserved stock for cancelled order: {}", order.getOrderNumber());
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.releaseReservedStock(item.getProduct().getId(), item.getQuantity());
        }
    }

    private void restockReturnedItems(Order order) {
        log.info("Restocking returned items for order: {}", order.getOrderNumber());
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.restock(item.getProduct().getId(), item.getQuantity());
        }
    }

    private void restoreInventoryForCancellation(Order order) {
        if (order.getStatus() == OrderStatus.PENDING) {
            releaseOrderReservation(order);
        } else if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PROCESSING) {
            restockReturnedItems(order);
        }
    }

    // ========== OTHER METHODS (keep as is with minor fixes) ==========

    private String generateOrderNumber() {
        String orderNumber;
        do {
            orderNumber = String.format("ORD-%d-%s",
                    System.currentTimeMillis(),
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    private void validateOwnership(Order order, Long requestingUserId) {
        if (requestingUserId != null && !order.getUser().getId().equals(requestingUserId)) {
            log.warn("Security Alert: User {} tried to access Order {} belonging to User {}",
                    requestingUserId, order.getId(), order.getUser().getId());
            throw new ResourceNotFoundException("Order not found");
        }
    }

    // Keep other methods as they are...
    @Override
    public OrderResponse getOrderById(Long orderId, Long requestingUserId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validateOwnership(order, requestingUserId);
        return orderMapper.toResponse(order);
    }

    @Override
    public OrderResponse getOrderByOrderNumber(String orderNumber, Long requestingUserId) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validateOwnership(order, requestingUserId);
        return orderMapper.toResponse(order);
    }

    @Override
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toResponse);
    }

    @Override
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(orderMapper::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String reason, Long requestingUserId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validateOwnership(order, requestingUserId);

        if (order.getStatus() == OrderStatus.CANCELLED ||
                order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.SHIPPED) {
            throw new BadRequestException("Order cannot be cancelled in status: " + order.getStatus());
        }

        restoreInventoryForCancellation(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancelledAt(LocalDateTime.now());

        log.info("Order {} cancelled by user {}", orderId, requestingUserId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable).map(orderMapper::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse addTrackingInfo(Long orderId, String trackingNumber, String carrier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setTrackingNumber(trackingNumber);
        order.setCarrier(carrier);
        order.setStatus(OrderStatus.SHIPPED);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderAdmin(Long orderId, String adminNotes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setAdminNotes(adminNotes);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.FAILED) {
            throw new BadRequestException("Only Cancelled or Failed orders can be deleted");
        }
        orderRepository.delete(order);
    }

    @Override
    public List<OrderResponse> getUserOrdersByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findUserOrdersBetweenDates(userId, startDate, endDate)
                .stream().map(orderMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public Double getUserTotalSpent(Long userId) {
        Double total = orderRepository.getTotalSpentByUser(userId, OrderStatus.DELIVERED);
        return total != null ? total : 0.0;
    }

    @Override
    public Long getUserOrderCount(Long userId) {
        return orderRepository.countByUserId(userId);
    }
}