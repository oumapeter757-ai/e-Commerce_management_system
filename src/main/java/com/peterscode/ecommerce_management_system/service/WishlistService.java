package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.response.WishlistResponse;

import java.util.List;

public interface WishlistService {

    List<WishlistResponse> getWishlist(Long userId);

    WishlistResponse addToWishlist(Long userId, Long productId);

    void removeFromWishlist(Long userId, Long productId);

    void clearWishlist(Long userId);

    boolean isInWishlist(Long userId, Long productId);

    long getWishlistCount(Long userId);

    /** Move wishlist item to cart */
    void moveToCart(Long userId, Long productId);
}

