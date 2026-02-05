package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.request.CancelOrderRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing orders and checking status")
public class OrderController {

    private final OrderService orderService;

    // =================================================================================
    // PUBLIC / CUSTOMER ENDPOINTS
    // =================================================================================

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create a new order")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        OrderResponse order = orderService.createOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get logged-in user's orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            Pageable pageable,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        Page<OrderResponse> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("My orders retrieved successfully", orders));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get order by ID (Secure)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long orderId,
            Authentication authentication) {
        // Pass ID if Customer, Null if Admin (skips ownership check)
        Long secureUserId = getSecurityContextUserId(authentication);
        OrderResponse order = orderService.getOrderById(orderId, secureUserId);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get order by Order Number (Secure)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByOrderNumber(
            @PathVariable String orderNumber,
            Authentication authentication) {
        Long secureUserId = getSecurityContextUserId(authentication);
        OrderResponse order = orderService.getOrderByOrderNumber(orderNumber, secureUserId);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) CancelOrderRequest request,
            Authentication authentication) {
        Long secureUserId = getSecurityContextUserId(authentication);
        String reason = (request != null) ? request.getReason() : "No reason provided";

        OrderResponse order = orderService.cancelOrder(orderId, reason, secureUserId);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }

    @GetMapping("/user/date-range")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Filter my orders by date")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        List<OrderResponse> orders = orderService.getUserOrdersByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    // =================================================================================
    // USER STATISTICS
    // =================================================================================

    @GetMapping("/user/stats/total-spent")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get total amount spent by user")
    public ResponseEntity<ApiResponse<Double>> getUserTotalSpent(Authentication authentication) {
        Long userId = getUserId(authentication);
        Double totalSpent = orderService.getUserTotalSpent(userId);
        return ResponseEntity.ok(ApiResponse.success("Total spent retrieved successfully", totalSpent));
    }

    @GetMapping("/user/stats/count")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get total count of user's orders")
    public ResponseEntity<ApiResponse<Long>> getUserOrderCount(Authentication authentication) {
        Long userId = getUserId(authentication);
        Long count = orderService.getUserOrderCount(userId);
        return ResponseEntity.ok(ApiResponse.success("Order count retrieved successfully", count));
    }

    // =================================================================================
    // ADMIN ONLY ENDPOINTS
    // =================================================================================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get ALL orders (Admin Only)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(Pageable pageable) {
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success("All orders retrieved successfully", orders));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orders of a specific user (Admin Only)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByUserId(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<OrderResponse> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("User orders retrieved successfully", orders));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Filter all orders by status (Admin Only)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            Pageable pageable) {
        Page<OrderResponse> orders = orderService.getOrdersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status (Admin Only)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatus(orderId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", order));
    }

    @PutMapping("/{orderId}/tracking")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add tracking info (Admin Only)")
    public ResponseEntity<ApiResponse<OrderResponse>> addTrackingInfo(
            @PathVariable Long orderId,
            @Valid @RequestBody TrackingInfoRequest request) {
        OrderResponse order = orderService.addTrackingInfo(orderId, request.getTrackingNumber(), request.getCarrier());
        return ResponseEntity.ok(ApiResponse.success("Tracking info added successfully", order));
    }

    @PutMapping("/{orderId}/admin-notes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update internal admin notes (Admin Only)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateAdminNotes(
            @PathVariable Long orderId,
            @RequestParam String adminNotes) {
        OrderResponse order = orderService.updateOrderAdmin(orderId, adminNotes);
        return ResponseEntity.ok(ApiResponse.success("Admin notes updated successfully", order));
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an order (Admin Only - Cancelled orders only)")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("Order deleted successfully", null));
    }

    // =================================================================================
    // SECURITY HELPER METHODS
    // =================================================================================

    /**
     * Extract raw User ID from Authentication.
     */
    private Long getUserId(Authentication authentication) {
        if (authentication == null) return null;
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid User ID in token");
        }
    }

    /**
     * Helper to decide if we need to enforce ownership checks.
     * Returns User ID if the user is a CUSTOMER.
     * Returns NULL if the user is an ADMIN (Bypassing ownership check in Service).
     */
    private Long getSecurityContextUserId(Authentication authentication) {
        if (authentication == null) return null;

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return null; // Admins can see everything
        } else {
            return getUserId(authentication); // Customers can only see their own
        }
    }
}