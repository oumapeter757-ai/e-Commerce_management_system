package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.*;
import com.peterscode.ecommerce_management_system.mapper.OrderMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.OrderRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.OrderResponse;
import com.peterscode.ecommerce_management_system.model.entity.*;
import com.peterscode.ecommerce_management_system.model.enums.OrderStatus;
import com.peterscode.ecommerce_management_system.repository.*;
import com.peterscode.ecommerce_management_system.service.InventoryService;
import com.peterscode.ecommerce_management_system.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

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
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, Long userId) {
        log.info("Creating order for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address shippingAddress = addressRepository.findById(request.getShippingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));

        Address billingAddress = addressRepository.findById(request.getBillingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Billing address not found"));

        if (!shippingAddress.getUser().getId().equals(userId) || !billingAddress.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Address does not belong to user");
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING) // Initial status is PENDING
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .couponCode(request.getCouponCode())
                .customerNotes(request.getCustomerNotes())
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingCost(BigDecimal.ZERO)
                .build();

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {

            // 1. Reserve Stock (PENDING state)
            inventoryService.reserveStock(itemRequest.getProductId(), itemRequest.getQuantity());

            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.getProductId()));

            // Update stats
            productRepository.incrementSoldCount(product.getId(), itemRequest.getQuantity());

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

        order.calculateTotals();
        Order savedOrder = orderRepository.save(order);
        log.info("Order created: {}", savedOrder.getOrderNumber());
        return orderMapper.toResponse(savedOrder);
    }

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

        // Handle Inventory Logic before changing status
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
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus oldStatus = order.getStatus();

        // 1. Handle transitions related to Inventory
        if (oldStatus == OrderStatus.PENDING && newStatus == OrderStatus.CONFIRMED) {
            // Payment success: Permanently reduce stock (move from 'Reserved' to 'Sold')
            confirmOrderStock(order);
        } else if (newStatus == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED) {
            // Admin cancels: Restore stock
            restoreInventoryForCancellation(order);
            order.setCancelledAt(LocalDateTime.now());
        } else if (newStatus == OrderStatus.RETURNED && oldStatus != OrderStatus.RETURNED) {
            // Customer returned item: Restock it
            restockReturnedItems(order);
        } else if (newStatus == OrderStatus.FAILED && oldStatus == OrderStatus.PENDING) {
            // Payment failed: Release reservation
            releaseOrderReservation(order);
        }

        // 2. Handle timestamps
        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        order.setStatus(newStatus);
        return orderMapper.toResponse(orderRepository.save(order));
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

    // --- Inventory Helper Methods ---

    private void restoreInventoryForCancellation(Order order) {
        if (order.getStatus() == OrderStatus.PENDING) {
            // It was just reserved, so release the reservation
            releaseOrderReservation(order);
        } else if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PROCESSING) {
            // It was already deducted from stock, so we need to add it back
            restockReturnedItems(order);
        }
    }

    private void confirmOrderStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.confirmStockReduction(item.getProduct().getId(), item.getQuantity());
        }
    }

    private void releaseOrderReservation(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.releaseReservedStock(item.getProduct().getId(), item.getQuantity());
        }
    }

    private void restockReturnedItems(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.restock(item.getProduct().getId(), item.getQuantity());
        }
    }

    // --- Standard Helpers ---

    private void validateOwnership(Order order, Long requestingUserId) {
        if (requestingUserId != null && !order.getUser().getId().equals(requestingUserId)) {
            log.warn("Security Alert: User {} tried to access Order {} belonging to User {}",
                    requestingUserId, order.getId(), order.getUser().getId());
            throw new ResourceNotFoundException("Order not found");
        }
    }

    private String generateOrderNumber() {
        String orderNumber;
        do {
            orderNumber = String.format("ORD-%d-%s",
                    System.currentTimeMillis(),
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
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