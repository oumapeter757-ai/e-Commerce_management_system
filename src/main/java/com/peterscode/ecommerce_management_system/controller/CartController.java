package com.peterscode.ecommerce_management_system.controller;


import com.peterscode.ecommerce_management_system.model.dto.request.CartItemRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.CartResponse;
import com.peterscode.ecommerce_management_system.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart Management", description = "APIs for shopping cart operations")
public class CartController {

    private final CartService cartService;

    // =================================================================================
    // AUTHENTICATED USER ENDPOINTS (Requires ROLE_CUSTOMER)
    // =================================================================================

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my cart", description = "Retrieve the current logged-in user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cart));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add item to my cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @Valid @RequestBody CartItemRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.addItemToCart(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cart));
    }

    @PutMapping("/items/{cartItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.updateCartItem(userId, cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cart updated successfully", cart));
    }

    @DeleteMapping("/items/{cartItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItemFromCart(
            @PathVariable Long cartItemId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.removeItemFromCart(userId, cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cart));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", cart));
    }

    @PostMapping("/merge")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Merge guest cart", description = "Merge a temporary guest cart into the user's permanent cart")
    public ResponseEntity<ApiResponse<CartResponse>> mergeCart(
            @RequestParam String sessionId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        cartService.mergeGuestCartWithUserCart(sessionId, userId);

        // Return the updated user cart
        CartResponse cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Carts merged successfully", cart));
    }

    // =================================================================================
    // GUEST ENDPOINTS (Public - tracked by Session ID)
    // =================================================================================

    @GetMapping("/guest")
    @Operation(summary = "Get guest cart")
    public ResponseEntity<ApiResponse<CartResponse>> getGuestCart(
            @RequestHeader("X-Session-ID") String sessionId) {
        CartResponse cart = cartService.getCartBySessionId(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Guest cart retrieved", cart));
    }

    @PostMapping("/guest/items")
    @Operation(summary = "Add item to guest cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToGuestCart(
            @RequestHeader("X-Session-ID") String sessionId,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse cart = cartService.addItemToGuestCart(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success("Item added to guest cart", cart));
    }

    // You can add Update/Delete for guest here if needed, mirroring the user endpoints
    // but accepting 'sessionId' instead of using 'Authentication'

    // =================================================================================
    // HELPER METHODS
    // =================================================================================

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