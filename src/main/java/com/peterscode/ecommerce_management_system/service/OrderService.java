package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.request.OrderRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.OrderResponse;
import com.peterscode.ecommerce_management_system.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request, Long userId);

    // UPDATED: Now accepts requestingUserId for security check
    OrderResponse getOrderById(Long orderId, Long requestingUserId);

    // UPDATED: Now accepts requestingUserId
    OrderResponse getOrderByOrderNumber(String orderNumber, Long requestingUserId);

    Page<OrderResponse> getAllOrders(Pageable pageable);

    Page<OrderResponse> getUserOrders(Long userId, Pageable pageable);

    Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);

    // UPDATED: Now accepts requestingUserId
    OrderResponse cancelOrder(Long orderId, String reason, Long requestingUserId);

    OrderResponse addTrackingInfo(Long orderId, String trackingNumber, String carrier);

    OrderResponse updateOrderAdmin(Long orderId, String adminNotes);

    List<OrderResponse> getUserOrdersByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    Double getUserTotalSpent(Long userId);

    Long getUserOrderCount(Long userId);

    void deleteOrder(Long orderId);
}