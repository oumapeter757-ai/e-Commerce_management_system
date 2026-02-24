package com.peterscode.ecommerce_management_system.model.dto.common;

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
public class CartItemDTO {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private String productBrand;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Integer availableStock;
    private Boolean isAvailable;

    // Kilimall-style features
    private Boolean selected;           // Selected for checkout
    private Boolean savedForLater;      // Saved for later
    private BigDecimal priceAtAddition;  // Price when added to cart
    private BigDecimal currentPrice;     // Current product price
    private Boolean priceChanged;        // Has price changed since adding?
    private BigDecimal priceDifference;  // Amount of price change
    private String notes;                // Color/size/variant specs
    private Integer maxBuyQuantity;      // Max purchasable quantity
    private BigDecimal discountPercentage; // Active discount percentage

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}