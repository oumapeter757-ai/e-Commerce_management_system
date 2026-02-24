package com.peterscode.ecommerce_management_system.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString(exclude = {"cart", "product"})
@Table(name = "cart_items", indexes = {
        @Index(name = "idx_cart_id", columnList = "cart_id"),
        @Index(name = "idx_product_id", columnList = "product_id")
})
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice; // Price at the time of adding to cart

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // Kilimall-style features
    @Column(name = "selected", nullable = false)
    @Builder.Default
    private Boolean selected = true; // Selected for checkout

    @Column(name = "saved_for_later", nullable = false)
    @Builder.Default
    private Boolean savedForLater = false; // Save for later feature

    @Column(name = "price_at_addition", precision = 10, scale = 2)
    private BigDecimal priceAtAddition; // Price when item was added (for price change detection)

    @Column(name = "notes", length = 500)
    private String notes; // Color/size/variant specifications

    @Column(name = "max_buy_quantity")
    private Integer maxBuyQuantity; // Per-product purchase limit

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business logic
    public void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}