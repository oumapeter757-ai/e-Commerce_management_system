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
public class RecentlyViewedResponse {

    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private String productBrand;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal discountPercentage;
    private Boolean isInStock;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private LocalDateTime viewedAt;
}

