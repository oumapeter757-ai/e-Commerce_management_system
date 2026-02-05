package com.peterscode.ecommerce_management_system.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_name", columnList = "name"),
        @Index(name = "idx_product_sku", columnList = "sku"),
        @Index(name = "idx_category_id", columnList = "category_id"),
        @Index(name = "idx_seller_id", columnList = "seller_id"),
        @Index(name = "idx_is_active", columnList = "is_active"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String sku; // Stock Keeping Unit

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller; // The admin/seller who added this product

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "additional_images", columnDefinition = "TEXT")
    private String additionalImages; // JSON array or comma-separated URLs

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(length = 100)
    private String manufacturer;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ZERO; // In kg

    @Column(length = 100)
    private String dimensions; // e.g., "10x20x30 cm"

    @Column(length = 50)
    private String color;

    @Column(length = 50)
    private String size;

    @Column(columnDefinition = "TEXT")
    private String tags; // Comma-separated tags for search

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "sold_count")
    @Builder.Default
    private Long soldCount = 0L;

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business logic methods
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    public boolean isLowStock() {
        return stockQuantity != null && lowStockThreshold != null
                && stockQuantity <= lowStockThreshold;
    }

    public BigDecimal getActualPrice() {
        return (discountPrice != null && discountPrice.compareTo(BigDecimal.ZERO) > 0)
                ? discountPrice
                : price;
    }

    public BigDecimal getDiscountPercentage() {
        if (discountPrice == null || price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return price.subtract(discountPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(price, 2, java.math.RoundingMode.HALF_UP);
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0L : this.viewCount) + 1;
    }

    public void incrementSoldCount(int quantity) {
        this.soldCount = (this.soldCount == null ? 0L : this.soldCount) + quantity;
    }

    public void reduceStock(int quantity) {
        if (this.stockQuantity != null && this.stockQuantity >= quantity) {
            this.stockQuantity -= quantity;
        } else {
            throw new IllegalStateException("Insufficient stock available");
        }
    }

    public void addStock(int quantity) {
        this.stockQuantity = (this.stockQuantity == null ? 0 : this.stockQuantity) + quantity;
    }
}