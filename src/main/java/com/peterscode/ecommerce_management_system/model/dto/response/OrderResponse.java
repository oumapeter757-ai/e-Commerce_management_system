package com.peterscode.ecommerce_management_system.model.dto.response;


import com.peterscode.ecommerce_management_system.model.dto.common.AddressDTO;
import com.peterscode.ecommerce_management_system.model.dto.common.OrderItemDTO;
import com.peterscode.ecommerce_management_system.model.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Long userId;
    private String userName;
    private List<OrderItemDTO> orderItems;
    private OrderStatus status;
    private AddressDTO shippingAddress;
    private AddressDTO billingAddress;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private String couponCode;
    private String customerNotes;
    private String adminNotes;
    private String trackingNumber;
    private String carrier;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private Integer totalItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}