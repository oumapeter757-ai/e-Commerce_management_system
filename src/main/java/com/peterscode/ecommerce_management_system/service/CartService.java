package com.peterscode.ecommerce_management_system.service;


import com.peterscode.ecommerce_management_system.model.dto.request.CartItemRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.CartResponse;

import java.util.List;

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

    // ==================== KILIMALL-STYLE FEATURES ====================

    /** Toggle selection state of specific cart items */
    CartResponse toggleItemSelection(Long userId, List<Long> itemIds, boolean selected);

    /** Select all active cart items */
    CartResponse selectAllItems(Long userId);

    /** Deselect all cart items */
    CartResponse deselectAllItems(Long userId);

    /** Bulk remove selected items from cart */
    CartResponse removeSelectedItems(Long userId, List<Long> itemIds);

    /** Move item to "Save for Later" */
    CartResponse saveForLater(Long userId, Long cartItemId);

    /** Move item from "Save for Later" back to active cart */
    CartResponse moveToCart(Long userId, Long cartItemId);

    /** Update item notes (color/size/variant specifications) */
    CartResponse updateItemNotes(Long userId, Long cartItemId, String notes);

    /** Get cart summary with price change detection alerts */
    CartResponse getCartSummary(Long userId);

    /** Estimate shipping cost for cart based on address */
    CartResponse estimateShipping(Long userId, Long addressId);
}