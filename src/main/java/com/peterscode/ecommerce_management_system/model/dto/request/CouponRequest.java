package com.peterscode.ecommerce_management_system.model.dto.request;

import com.peterscode.ecommerce_management_system.model.enums.DiscountType;
import jakarta.validation.constraints.*;
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
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50, message = "Coupon code must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Coupon code must contain only uppercase letters, numbers, hyphens, and underscores")
    private String code;

    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.01", message = "Maximum discount amount must be greater than 0")
    private BigDecimal maxDiscountAmount;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    @Min(value = 1, message = "Per user limit must be at least 1")
    private Integer perUserLimit;

    @NotNull(message = "Start date is required")
    private LocalDateTime startsAt;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiresAt;
}

