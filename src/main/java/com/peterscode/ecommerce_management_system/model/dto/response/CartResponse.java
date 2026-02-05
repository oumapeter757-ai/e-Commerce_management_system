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
    private Integer totalItems;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String couponCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}