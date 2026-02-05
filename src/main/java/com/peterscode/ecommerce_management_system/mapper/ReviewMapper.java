package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.request.ReviewRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ReviewResponse;
import com.peterscode.ecommerce_management_system.model.entity.Review;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userName(review.getUser() != null ? review.getUser().getFirstName() + " "+ review.getUser().getLastName() : null)
                .orderId(review.getOrder() != null ? review.getOrder().getId() : null)
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .verifiedPurchase(review.getVerifiedPurchase())
                .isApproved(review.getIsApproved())
                .helpfulCount(review.getHelpfulCount())
                .notHelpfulCount(review.getNotHelpfulCount())
                .images(parseImages(review.getImages()))
                .adminResponse(review.getAdminResponse())
                .respondedAt(review.getRespondedAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    public Review toEntity(ReviewRequest request) {
        if (request == null) {
            return null;
        }

        return Review.builder()
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .images(serializeImages(request.getImages()))
                .verifiedPurchase(false)
                .isApproved(false)
                .helpfulCount(0)
                .notHelpfulCount(0)
                .build();
    }

    private List<String> parseImages(String images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(images.split(","));
    }

    private String serializeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return String.join(",", images);
    }
}