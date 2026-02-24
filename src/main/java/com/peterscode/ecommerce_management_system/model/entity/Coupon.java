package com.peterscode.ecommerce_management_system.model.entity;

import com.peterscode.ecommerce_management_system.model.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupon_code", columnList = "code"),
        @Index(name = "idx_coupon_active", columnList = "is_active"),
        @Index(name = "idx_coupon_expires", columnList = "expires_at")
})
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "per_user_limit")
    @Builder.Default
    private Integer perUserLimit = 1;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== BUSINESS LOGIC =====

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(isActive)
                && now.isAfter(startsAt)
                && now.isBefore(expiresAt)
                && (usageLimit == null || usageCount < usageLimit);
    }


    public boolean meetsMinimumOrder(BigDecimal orderTotal) {
        return minOrderAmount == null || orderTotal.compareTo(minOrderAmount) >= 0;
    }

    /**
     * Calculate the discount amount for a given order total.
     */
    public BigDecimal calculateDiscount(BigDecimal orderTotal) {
        if (!isValid() || !meetsMinimumOrder(orderTotal)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        switch (discountType) {
            case PERCENTAGE:
                discount = orderTotal.multiply(discountValue)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                break;
            case FIXED_AMOUNT:
                discount = discountValue;
                break;
            case FREE_SHIPPING:
                return BigDecimal.ZERO; // Handled separately in shipping calculation
            default:
                return BigDecimal.ZERO;
        }

        // Cap at max discount amount
        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }

        // Discount cannot exceed order total
        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }

        return discount;
    }

    public void incrementUsage() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }
}

