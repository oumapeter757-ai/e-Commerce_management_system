package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.common.CartItemDTO;
import com.peterscode.ecommerce_management_system.model.dto.response.CartResponse;
import com.peterscode.ecommerce_management_system.model.entity.Cart;
import com.peterscode.ecommerce_management_system.model.entity.CartItem;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return null;
        }

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .sessionId(cart.getSessionId())
                .items(cart.getItems() != null
                        ? cart.getItems().stream().map(this::toCartItemDTO).collect(Collectors.toList())
                        : Collections.emptyList())
                .totalItems(cart.getTotalItems())
                .subtotal(cart.getSubtotal())
                .discountAmount(cart.getDiscountAmount())
                .taxAmount(cart.getTaxAmount())
                .totalAmount(cart.getTotalAmount())
                .couponCode(cart.getCouponCode())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    public CartItemDTO toCartItemDTO(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }

        Product product = cartItem.getProduct();

        return CartItemDTO.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProduct() != null ? cartItem.getProduct().getId() : null)
                .productName(cartItem.getProduct() != null ? cartItem.getProduct().getName() : null)
                .productSku(cartItem.getProduct() != null ? cartItem.getProduct().getSku() : null)
                .productImageUrl(cartItem.getProduct() != null ? cartItem.getProduct().getImageUrl() : null)
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .totalPrice(cartItem.getTotalPrice())
                .availableStock(product != null ? product.getStockQuantity() : 0)
                .isAvailable(product != null && Boolean.TRUE.equals(product.getIsActive()))
                .createdAt(cartItem.getCreatedAt())
                .updatedAt(cartItem.getUpdatedAt())
                .build();
    }
}