package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.RecentlyViewedResponse;
import com.peterscode.ecommerce_management_system.service.RecentlyViewedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recently-viewed")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Recently Viewed", description = "APIs for tracking recently viewed products")
public class RecentlyViewedController {

    private final RecentlyViewedService recentlyViewedService;

    @GetMapping
    @Operation(summary = "Get recently viewed products")
    public ResponseEntity<ApiResponse<List<RecentlyViewedResponse>>> getRecentlyViewed(
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        List<RecentlyViewedResponse> items = recentlyViewedService.getRecentlyViewed(userId, limit);
        return ResponseEntity.ok(ApiResponse.success("Recently viewed products retrieved", items));
    }

    @PostMapping("/{productId}")
    @Operation(summary = "Track product view")
    public ResponseEntity<ApiResponse<Void>> trackView(
            @PathVariable Long productId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        recentlyViewedService.trackProductView(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product view tracked", null));
    }

    @DeleteMapping
    @Operation(summary = "Clear viewing history")
    public ResponseEntity<ApiResponse<Void>> clearHistory(Authentication authentication) {
        Long userId = getUserId(authentication);
        recentlyViewedService.clearHistory(userId);
        return ResponseEntity.ok(ApiResponse.success("Viewing history cleared", null));
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

