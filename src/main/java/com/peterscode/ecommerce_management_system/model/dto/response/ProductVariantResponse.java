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
public class ProductVariantResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private String variantName;
    private String color;
    private String size;
    private String material;
    private BigDecimal priceAdjustment;
    private BigDecimal effectivePrice;
    private Integer stockQuantity;
    private Boolean isInStock;
    private String imageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

