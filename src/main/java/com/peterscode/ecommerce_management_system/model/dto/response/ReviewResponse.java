package com.peterscode.ecommerce_management_system.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Long userId;
    private String userName;
    private Long orderId;
    private Integer rating;
    private String title;
    private String comment;
    private Boolean verifiedPurchase;
    private Boolean isApproved;
    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private List<String> images;
    private String adminResponse;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}