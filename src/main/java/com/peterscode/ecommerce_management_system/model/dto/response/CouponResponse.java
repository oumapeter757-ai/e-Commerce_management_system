package com.peterscode.ecommerce_management_system.model.dto.response;

import com.peterscode.ecommerce_management_system.model.enums.DiscountType;
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
public class CouponResponse {

    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer usageCount;
    private Integer perUserLimit;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private Boolean isValid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

