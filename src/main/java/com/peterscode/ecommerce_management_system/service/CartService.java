package com.peterscode.ecommerce_management_system.service;


import com.peterscode.ecommerce_management_system.model.dto.request.CartItemRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse getCartBySessionId(String sessionId);

    CartResponse addItemToCart(Long userId, CartItemRequest request);

    CartResponse addItemToGuestCart(String sessionId, CartItemRequest request);

    CartResponse updateCartItem(Long userId, Long cartItemId, Integer quantity);

    CartResponse removeItemFromCart(Long userId, Long cartItemId);

    CartResponse clearCart(Long userId);

    CartResponse applyCoupon(Long userId, String couponCode);

    CartResponse removeCoupon(Long userId);

    void mergeGuestCartWithUserCart(String sessionId, Long userId);

    void deleteCart(Long userId);
}