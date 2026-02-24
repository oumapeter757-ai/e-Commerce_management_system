package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.request.WishlistRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.WishlistResponse;
import com.peterscode.ecommerce_management_system.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Wishlist Management", description = "APIs for managing product wishlists")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get my wishlist")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getWishlist(Authentication authentication) {
        Long userId = getUserId(authentication);
        List<WishlistResponse> wishlist = wishlistService.getWishlist(userId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved", wishlist));
    }

    @PostMapping
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @Valid @RequestBody WishlistRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        WishlistResponse response = wishlistService.addToWishlist(userId, request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product added to wishlist", response));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist", null));
    }

    @DeleteMapping
    @Operation(summary = "Clear entire wishlist")
    public ResponseEntity<ApiResponse<Void>> clearWishlist(Authentication authentication) {
        Long userId = getUserId(authentication);
        wishlistService.clearWishlist(userId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist cleared", null));
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "Check if product is in wishlist")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> isInWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        boolean inWishlist = wishlistService.isInWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist check",
                Map.of("inWishlist", inWishlist)));
    }

    @GetMapping("/count")
    @Operation(summary = "Get wishlist count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getWishlistCount(Authentication authentication) {
        Long userId = getUserId(authentication);
        long count = wishlistService.getWishlistCount(userId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist count", Map.of("count", count)));
    }

    @PostMapping("/{productId}/move-to-cart")
    @Operation(summary = "Move wishlist item to cart")
    public ResponseEntity<ApiResponse<Void>> moveToCart(
            @PathVariable Long productId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        wishlistService.moveToCart(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product moved to cart", null));
    }

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
}

