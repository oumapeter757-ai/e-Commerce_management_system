package com.peterscode.ecommerce_management_system.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString(exclude = {"product"})
@Table(name = "product_variants", indexes = {
        @Index(name = "idx_variant_product", columnList = "product_id"),
        @Index(name = "idx_variant_sku", columnList = "sku"),
        @Index(name = "idx_variant_active", columnList = "is_active")
})
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "variant_name", length = 100)
    private String variantName;

    @Column(length = 50)
    private String color;

    @Column(length = 50)
    private String size;

    @Column(length = 100)
    private String material;

    @Column(name = "price_adjustment", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

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

    public BigDecimal getEffectivePrice() {
        BigDecimal basePrice = product != null ? product.getActualPrice() : BigDecimal.ZERO;
        return basePrice.add(priceAdjustment != null ? priceAdjustment : BigDecimal.ZERO);
    }

    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }
}

