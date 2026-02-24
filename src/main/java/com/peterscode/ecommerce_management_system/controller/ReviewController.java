package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.request.ReviewRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.ReviewResponse;
import com.peterscode.ecommerce_management_system.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Review Management", description = "APIs for product reviews and ratings")
public class ReviewController {

    private final ReviewService reviewService;

    // ==================== PUBLIC ENDPOINTS ====================

    @GetMapping("/{reviewId}")
    @Operation(summary = "Get review by ID (Public)")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(@PathVariable Long reviewId) {
        ReviewResponse review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review retrieved successfully", review));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get approved reviews for a product (Public)")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getApprovedProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Product reviews retrieved", reviews));
    }

    @GetMapping("/product/{productId}/rating")
    @Operation(summary = "Get average rating for a product (Public)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductRating(@PathVariable Long productId) {
        Double avgRating = reviewService.getProductAverageRating(productId);
        Long count = reviewService.getProductReviewCount(productId);
        Map<String, Object> data = Map.of(
                "productId", productId,
                "averageRating", avgRating != null ? avgRating : 0.0,
                "reviewCount", count != null ? count : 0L
        );
        return ResponseEntity.ok(ApiResponse.success("Product rating retrieved", data));
    }

    // ==================== CUSTOMER ENDPOINTS ====================

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create a review (Customer)")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        ReviewResponse review = reviewService.createReview(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", review));
    }

    @GetMapping("/my-reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my reviews (Customer)")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getMyReviews(
            Authentication authentication,
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = getUserId(authentication);
        Page<ReviewResponse> reviews = reviewService.getUserReviews(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Your reviews retrieved", reviews));
    }

    @PostMapping("/{reviewId}/helpful")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Mark review as helpful")
    public ResponseEntity<ApiResponse<ReviewResponse>> markHelpful(
            @PathVariable Long reviewId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        ReviewResponse review = reviewService.markReviewHelpful(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("Review marked as helpful", review));
    }

    @PostMapping("/{reviewId}/not-helpful")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Mark review as not helpful")
    public ResponseEntity<ApiResponse<ReviewResponse>> markNotHelpful(
            @PathVariable Long reviewId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        ReviewResponse review = reviewService.markReviewNotHelpful(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("Review marked as not helpful", review));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Delete my review (Customer)")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully"));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending reviews (Admin)")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getPendingReviews(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getPendingReviews(pageable);
        return ResponseEntity.ok(ApiResponse.success("Pending reviews retrieved", reviews));
    }

    @PutMapping("/{reviewId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a review (Admin)")
    public ResponseEntity<ApiResponse<ReviewResponse>> approveReview(@PathVariable Long reviewId) {
        ReviewResponse review = reviewService.approveReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review approved", review));
    }

    @PutMapping("/{reviewId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a review (Admin)")
    public ResponseEntity<ApiResponse<ReviewResponse>> rejectReview(@PathVariable Long reviewId) {
        ReviewResponse review = reviewService.rejectReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review rejected", review));
    }

    @PutMapping("/{reviewId}/respond")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add admin response to a review (Admin)")
    public ResponseEntity<ApiResponse<ReviewResponse>> addAdminResponse(
            @PathVariable Long reviewId,
            @RequestParam String response) {
        ReviewResponse review = reviewService.addAdminResponse(reviewId, response);
        return ResponseEntity.ok(ApiResponse.success("Admin response added", review));
    }

    // ==================== HELPER ====================

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

