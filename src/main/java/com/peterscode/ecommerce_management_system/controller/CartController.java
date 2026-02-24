package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.request.BulkCartOperationRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.CartItemNoteRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.CartItemRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.CartItemSelectionRequest;
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
@Tag(name = "Cart Management", description = "Kilimall-style shopping cart APIs")
public class CartController {

    private final CartService cartService;

    // =================================================================================
    // CORE CART ENDPOINTS (ROLE_CUSTOMER)
    // =================================================================================

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my cart", description = "Retrieve the current user's cart with price change alerts")
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cart));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get cart summary", description = "Get cart summary with refreshed prices and savings")
    public ResponseEntity<ApiResponse<CartResponse>> getCartSummary(Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.getCartSummary(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart summary retrieved", cart));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add item to cart")
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

    // =================================================================================
    // KILIMALL-STYLE SELECTION ENDPOINTS
    // =================================================================================

    @PatchMapping("/items/select")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Toggle item selection", description = "Select/deselect specific items for checkout")
    public ResponseEntity<ApiResponse<CartResponse>> toggleItemSelection(
            @Valid @RequestBody CartItemSelectionRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.toggleItemSelection(userId, request.getItemIds(), request.getSelected());
        return ResponseEntity.ok(ApiResponse.success("Selection updated", cart));
    }

    @PostMapping("/items/select-all")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Select all items", description = "Select all active cart items for checkout")
    public ResponseEntity<ApiResponse<CartResponse>> selectAllItems(Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.selectAllItems(userId);
        return ResponseEntity.ok(ApiResponse.success("All items selected", cart));
    }

    @PostMapping("/items/deselect-all")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Deselect all items")
    public ResponseEntity<ApiResponse<CartResponse>> deselectAllItems(Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.deselectAllItems(userId);
        return ResponseEntity.ok(ApiResponse.success("All items deselected", cart));
    }

    @DeleteMapping("/items/bulk")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Bulk remove items", description = "Remove multiple items at once")
    public ResponseEntity<ApiResponse<CartResponse>> removeSelectedItems(
            @Valid @RequestBody BulkCartOperationRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.removeSelectedItems(userId, request.getItemIds());
        return ResponseEntity.ok(ApiResponse.success("Selected items removed", cart));
    }

    // =================================================================================
    // SAVE FOR LATER ENDPOINTS (Kilimall feature)
    // =================================================================================

    @PostMapping("/items/{cartItemId}/save-for-later")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Save item for later", description = "Move item to 'Save for Later' section")
    public ResponseEntity<ApiResponse<CartResponse>> saveForLater(
            @PathVariable Long cartItemId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.saveForLater(userId, cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Item saved for later", cart));
    }

    @PostMapping("/items/{cartItemId}/move-to-cart")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Move back to cart", description = "Move item from 'Save for Later' back to active cart")
    public ResponseEntity<ApiResponse<CartResponse>> moveToCart(
            @PathVariable Long cartItemId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.moveToCart(userId, cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Item moved to cart", cart));
    }

    // =================================================================================
    // ITEM NOTES (Color/Size/Variant specs)
    // =================================================================================

    @PatchMapping("/items/{cartItemId}/notes")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update item notes", description = "Add color/size/variant specifications")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemNotes(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemNoteRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.updateItemNotes(userId, cartItemId, request.getNotes());
        return ResponseEntity.ok(ApiResponse.success("Notes updated", cart));
    }

    // =================================================================================
    // COUPON & SHIPPING ENDPOINTS
    // =================================================================================

    @PostMapping("/coupon")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Apply coupon code")
    public ResponseEntity<ApiResponse<CartResponse>> applyCoupon(
            @RequestParam String couponCode,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.applyCoupon(userId, couponCode);
        return ResponseEntity.ok(ApiResponse.success("Coupon applied", cart));
    }

    @DeleteMapping("/coupon")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Remove coupon code")
    public ResponseEntity<ApiResponse<CartResponse>> removeCoupon(Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.removeCoupon(userId);
        return ResponseEntity.ok(ApiResponse.success("Coupon removed", cart));
    }

    @GetMapping("/shipping-estimate")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Estimate shipping cost", description = "Calculate shipping based on delivery address")
    public ResponseEntity<ApiResponse<CartResponse>> estimateShipping(
            @RequestParam Long addressId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CartResponse cart = cartService.estimateShipping(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Shipping estimate calculated", cart));
    }

    // =================================================================================
    // CART MERGE (Login flow)
    // =================================================================================

    @PostMapping("/merge")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Merge guest cart", description = "Merge a guest cart into the user's permanent cart after login")
    public ResponseEntity<ApiResponse<CartResponse>> mergeCart(
            @RequestParam String sessionId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        cartService.mergeGuestCartWithUserCart(sessionId, userId);
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