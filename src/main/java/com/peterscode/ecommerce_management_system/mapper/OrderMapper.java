package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.common.OrderItemDTO;
import com.peterscode.ecommerce_management_system.model.dto.response.OrderResponse;
import com.peterscode.ecommerce_management_system.model.entity.Order;
import com.peterscode.ecommerce_management_system.model.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OrderMapper {

    private final AddressMapper addressMapper;

    public OrderMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .userName(order.getUser() != null ? order.getUser().getFirstName() + " " + order.getUser().getLastName() : null)
                .orderItems(order.getOrderItems().stream()
                        .map(this::toOrderItemDTO)
                        .collect(Collectors.toList()))
                .status(order.getStatus())
                .shippingAddress(addressMapper.toDTO(order.getShippingAddress()))
                .billingAddress(addressMapper.toDTO(order.getBillingAddress()))
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .shippingCost(order.getShippingCost())
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCouponCode())
                .customerNotes(order.getCustomerNotes())
                .adminNotes(order.getAdminNotes())
                .trackingNumber(order.getTrackingNumber())
                .carrier(order.getCarrier())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())
                .totalItems(order.getTotalItems())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderItemDTO toOrderItemDTO(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        return OrderItemDTO.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct() != null ? orderItem.getProduct().getId() : null)
                .productName(orderItem.getProductName())
                .productSku(orderItem.getProductSku())
                .productImageUrl(orderItem.getProductImageUrl())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .discountPrice(orderItem.getDiscountPrice())
                .totalPrice(orderItem.getTotalPrice())
                .taxAmount(orderItem.getTaxAmount())
                .createdAt(orderItem.getCreatedAt())
                .build();
    }
}