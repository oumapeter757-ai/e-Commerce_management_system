package com.peterscode.ecommerce_management_system.service;


import com.peterscode.ecommerce_management_system.model.dto.request.ReviewRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(ReviewRequest request, Long userId);

    ReviewResponse getReviewById(Long reviewId);

    Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable);

    Page<ReviewResponse> getApprovedProductReviews(Long productId, Pageable pageable);

    Page<ReviewResponse> getUserReviews(Long userId, Pageable pageable);

    Page<ReviewResponse> getPendingReviews(Pageable pageable);

    ReviewResponse approveReview(Long reviewId);

    ReviewResponse rejectReview(Long reviewId);

    ReviewResponse addAdminResponse(Long reviewId, String response);

    // --- VOTING (Updated to require UserId) ---
    // Removed the old 'markReviewHelpful(Long reviewId)' methods

    ReviewResponse markReviewHelpful(Long reviewId, Long userId);

    ReviewResponse markReviewNotHelpful(Long reviewId, Long userId);
    // ------------------------------------------

    Double getProductAverageRating(Long productId);

    Long getProductReviewCount(Long productId);

    void deleteReview(Long reviewId, Long userId);

    boolean hasUserReviewedProduct(Long userId, Long productId, Long orderId);
}