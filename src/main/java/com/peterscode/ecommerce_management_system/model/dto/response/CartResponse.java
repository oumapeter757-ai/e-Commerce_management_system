package com.peterscode.ecommerce_management_system.model.dto.response;


import com.peterscode.ecommerce_management_system.model.dto.common.CartItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long id;
    private Long userId;
    private String sessionId;
    private List<CartItemDTO> items;
    private List<CartItemDTO> savedForLaterItems;  // Kilimall: saved for later section
    private Integer totalItems;
    private Integer selectedItemCount;              // Kilimall: number of selected items
    private BigDecimal subtotal;
    private BigDecimal selectedItemsTotal;           // Kilimall: only selected items total
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal estimatedShippingCost;         // Kilimall: shipping estimate
    private BigDecimal totalAmount;
    private BigDecimal totalSavings;                  // Kilimall: you save X amount
    private String couponCode;
    private List<CartItemDTO> priceChangedItems;      // Kilimall: items with price changes
    private LocalDateTime expiresAt;                   // Guest cart expiry
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}