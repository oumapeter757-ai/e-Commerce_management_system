package com.peterscode.ecommerce_management_system.model.entity;

import com.peterscode.ecommerce_management_system.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString(exclude = {"product"})
@Table(name = "inventory", indexes = {
        @Index(name = "idx_product_id", columnList = "product_id"),
        @Index(name = "idx_sku", columnList = "sku")
})
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private String sku;

    @Column(name = "available_stock", nullable = false)
    private Integer availableStock;

    @Column(name = "reserved_stock", nullable = false)
    @Builder.Default
    private Integer reservedStock = 0;

    @Column(name = "low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Column(name = "restock_quantity")
    private Integer restockQuantity;

    @Column(name = "last_restocked")
    private LocalDateTime lastRestocked;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    /**
     * Check if stock is low
     */
    public boolean isLowStock() {
        return availableStock <= lowStockThreshold;
    }

    public void confirmReservedQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (reservedStock < quantity) {
            throw new InsufficientStockException(
                    String.format("Cannot confirm %d units. Only %d units are reserved.",
                            quantity, reservedStock)
            );
        }

        reservedStock -= quantity;
    }

    /**
     * Reserve quantity for an order - move from available to reserved
     */
    public void reserveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (availableStock < quantity) {
            throw new InsufficientStockException(
                    String.format("Cannot reserve %d units. Only %d units available.",
                            quantity, availableStock)
            );
        }

        availableStock -= quantity;
        reservedStock += quantity;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Release reserved quantity - move from reserved back to available
     */
    public void releaseReservedQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (reservedStock < quantity) {
            throw new InsufficientStockException(
                    String.format("Cannot release %d units. Only %d units are reserved.",
                            quantity, reservedStock)
            );
        }

        reservedStock -= quantity;
        availableStock += quantity;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Add quantity to available stock
     */
    public void addQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        availableStock += quantity;
        updatedAt = LocalDateTime.now();

        if (quantity.equals(restockQuantity)) {
            lastRestocked = LocalDateTime.now();
        }
    }

    /**
     * Check if sufficient stock is available
     */
    public boolean hasSufficientStock(Integer requiredQuantity) {
        if (requiredQuantity == null || requiredQuantity <= 0) {
            return false;
        }
        return availableStock >= requiredQuantity;
    }
}