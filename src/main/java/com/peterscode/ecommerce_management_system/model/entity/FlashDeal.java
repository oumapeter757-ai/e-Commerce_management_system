package com.peterscode.ecommerce_management_system.model.entity;

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
@ToString(exclude = {"product"})
@Table(name = "flash_deals", indexes = {
        @Index(name = "idx_flash_product", columnList = "product_id"),
        @Index(name = "idx_flash_active", columnList = "is_active"),
        @Index(name = "idx_flash_dates", columnList = "starts_at, ends_at")
})
public class FlashDeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "deal_name", length = 255)
    private String dealName;

    @Column(name = "deal_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dealPrice;

    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "stock_limit")
    private Integer stockLimit;

    @Column(name = "sold_count")
    @Builder.Default
    private Integer soldCount = 0;

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

    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(isActive)
                && now.isAfter(startsAt)
                && now.isBefore(endsAt);
    }


    public BigDecimal getCalculatedDiscountPercentage() {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return originalPrice.subtract(dealPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 2, RoundingMode.HALF_UP);
    }

    public void incrementSoldCount() {
        this.soldCount = (this.soldCount == null ? 0 : this.soldCount) + 1;
    }

    public long getRemainingTimeInSeconds() {
        if (endsAt == null) return 0;
        long remaining = java.time.Duration.between(LocalDateTime.now(), endsAt).getSeconds();
        return Math.max(remaining, 0);
    }

    public Integer getRemainingStock() {
        if (stockLimit == null) return null;
        return Math.max(stockLimit - (soldCount != null ? soldCount : 0), 0);
    }
}

