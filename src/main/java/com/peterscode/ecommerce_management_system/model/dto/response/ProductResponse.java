package com.peterscode.ecommerce_management_system.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String sku;
    private String description;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal actualPrice;
    private BigDecimal discountPercentage;
    private CategoryResponse category;
    private SellerInfo seller;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private Boolean inStock;
    private Boolean lowStock;
    private String imageUrl;
    private String additionalImages;
    private String brand;
    private String manufacturer;
    private BigDecimal weight;
    private String dimensions;
    private String color;
    private String size;
    private String tags;
    private Boolean isActive;
    private Boolean isFeatured;
    private Long viewCount;
    private Long soldCount;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerInfo implements Serializable {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
    }
}