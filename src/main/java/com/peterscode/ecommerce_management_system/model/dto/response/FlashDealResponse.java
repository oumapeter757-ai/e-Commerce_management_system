package com.peterscode.ecommerce_management_system.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashDealResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private String dealName;
    private BigDecimal dealPrice;
    private BigDecimal originalPrice;
    private BigDecimal discountPercentage;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Long remainingTimeInSeconds;
    private Integer stockLimit;
    private Integer soldCount;
    private Integer remainingStock;
    private Boolean isActive;
    private Boolean isCurrentlyActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

