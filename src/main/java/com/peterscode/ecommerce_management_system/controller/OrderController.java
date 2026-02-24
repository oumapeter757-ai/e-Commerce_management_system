package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.request.CancelOrderRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.CheckoutRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.OrderRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.TrackingInfoRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.UpdateStatusRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.OrderResponse;
import com.peterscode.ecommerce_management_system.model.enums.OrderStatus;
import com.peterscode.ecommerce_management_system.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Order Controller - Clean API layer without business logic
 * All business logic is handled in OrderService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for creating and managing orders")
public class OrderController {

    private final OrderService orderService;

    // ==================== CUSTOMER ENDPOINTS ====================

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create new order (Customer)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        log.info("Creating order for user: {}", userId);

        OrderResponse order = orderService.createOrder(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully. Please proceed to payment.", order));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Checkout from cart (Kilimall-style: selected items only)")
    public ResponseEntity<ApiResponse<OrderResponse>> checkoutFromCart(
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        log.info("Kilimall checkout for user: {}", userId);

        OrderResponse order = orderService.checkoutFromCart(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully from cart. Please proceed to payment.", order));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my orders (Customer)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        log.debug("Fetching orders for user: {}", userId);

        Page<OrderResponse> orders = orderService.getUserOrders(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long orderId,
            Authentication authentication) {

        Long userId = getUserIdOrNull(authentication);
        log.debug("Fetching order: {} for user: {}", orderId, userId);

        OrderResponse order = orderService.getOrderById(orderId, userId);

        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByOrderNumber(
            @PathVariable String orderNumber,
            Authentication authentication) {

        Long userId = getUserIdOrNull(authentication);
        log.debug("Fetching order by number: {} for user: {}", orderNumber, userId);

        OrderResponse order = orderService.getOrderByOrderNumber(orderNumber, userId);

        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Cancel order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) CancelOrderRequest request,
            Authentication authentication) {

        Long userId = getUserIdOrNull(authentication);
        String reason = (request != null && request.getReason() != null)
                ? request.getReason()
                : "Cancelled by user";

        log.info("Cancelling order: {} - Reason: {}", orderId, reason);

        OrderResponse order = orderService.cancelOrder(orderId, reason, userId);

        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }

    @GetMapping("/user/date-range")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my orders by date range")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        log.debug("Fetching orders for user: {} between {} and {}", userId, startDate, endDate);

        List<OrderResponse> orders = orderService.getUserOrdersByDateRange(userId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(
                "Orders retrieved successfully (" + orders.size() + " orders)", orders));
    }

    // ==================== USER STATISTICS ====================

    @GetMapping("/user/stats/total-spent")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get total amount spent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserTotalSpent(
            Authentication authentication) {

        Long userId = getUserId(authentication);
        Double totalSpent = orderService.getUserTotalSpent(userId);

        Map<String, Object> stats = Map.of(
                "userId", userId,
                "totalSpent", totalSpent,
                "currency", "KES"
        );

        return ResponseEntity.ok(ApiResponse.success("Total spent retrieved", stats));
    }

    @GetMapping("/user/stats/count")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get total order count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserOrderCount(
            Authentication authentication) {

        Long userId = getUserId(authentication);
        Long count = orderService.getUserOrderCount(userId);

        Map<String, Object> stats = Map.of(
                "userId", userId,
                "totalOrders", count
        );

        return ResponseEntity.ok(ApiResponse.success("Order count retrieved", stats));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders (Admin only)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Admin fetching all orders - Page: {}", pageable.getPageNumber());

        Page<OrderResponse> orders = orderService.getAllOrders(pageable);

        return ResponseEntity.ok(ApiResponse.success(
                "All orders retrieved (" + orders.getTotalElements() + " total)", orders));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orders by user ID (Admin only)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Admin fetching orders for user: {}", userId);

        Page<OrderResponse> orders = orderService.getUserOrders(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                "User orders retrieved (" + orders.getTotalElements() + " orders)", orders));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orders by status (Admin only)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Admin fetching orders with status: {}", status);

        Page<OrderResponse> orders = orderService.getOrdersByStatus(status, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                "Orders with status " + status + " retrieved (" + orders.getTotalElements() + " orders)",
                orders));
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status (Admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateStatusRequest request) {

        log.info("Admin updating order: {} to status: {}", orderId, request.getStatus());

        OrderResponse order = orderService.updateOrderStatus(orderId, request.getStatus());

        return ResponseEntity.ok(ApiResponse.success(
                "Order status updated to " + request.getStatus(), order));
    }

    @PutMapping("/{orderId}/tracking")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add tracking information (Admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> addTrackingInfo(
            @PathVariable Long orderId,
            @Valid @RequestBody TrackingInfoRequest request) {

        log.info("Admin adding tracking info for order: {} - Tracking: {}, Carrier: {}",
                orderId, request.getTrackingNumber(), request.getCarrier());

        OrderResponse order = orderService.addTrackingInfo(
                orderId,
                request.getTrackingNumber(),
                request.getCarrier());

        return ResponseEntity.ok(ApiResponse.success(
                "Tracking information added successfully", order));
    }

    @PutMapping("/{orderId}/admin-notes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update admin notes (Admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateAdminNotes(
            @PathVariable Long orderId,
            @RequestParam String adminNotes) {

        log.info("Admin updating notes for order: {}", orderId);

        OrderResponse order = orderService.updateOrderAdmin(orderId, adminNotes);

        return ResponseEntity.ok(ApiResponse.success("Admin notes updated successfully", order));
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete order (Admin only - Cancelled/Failed orders only)")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long orderId) {

        log.warn("Admin deleting order: {}", orderId);

        orderService.deleteOrder(orderId);

        return ResponseEntity.ok(ApiResponse.success("Order deleted successfully", null));
    }

    // ==================== HELPER METHODS ====================

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

    private Long getUserIdOrNull(Authentication authentication) {
        if (authentication == null) return null;
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

