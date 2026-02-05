package com.peterscode.ecommerce_management_system.model.entity;

import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Version // Optimistic Locking
    private Long version;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Helper method to calculate actually sellable stock
    public int getAvailableStock() {
        return this.quantity - this.reservedQuantity;
    }

    public boolean isLowStock() {
        return getAvailableStock() <= lowStockThreshold;
    }

    /**
     * Deduct inventory quantity after successful payment
     * This reduces both total quantity and reserved quantity
     *
     * @param quantityToDeduct Amount to deduct from inventory
     * @throws BadRequestException if insufficient stock
     */
    public void deductQuantity(Integer quantityToDeduct) {
        if (quantityToDeduct == null || quantityToDeduct <= 0) {
            throw new BadRequestException("Deduction quantity must be positive");
        }

        if (this.quantity < quantityToDeduct) {
            throw new BadRequestException(
                    String.format("Insufficient inventory. Available: %d, Requested: %d",
                            this.quantity, quantityToDeduct)
            );
        }

        // Reduce total quantity
        this.quantity -= quantityToDeduct;

        // Also reduce reserved quantity if it was previously reserved
        if (this.reservedQuantity >= quantityToDeduct) {
            this.reservedQuantity -= quantityToDeduct;
        } else {
            // If reserved is less than deduction, set to 0
            this.reservedQuantity = 0;
        }
    }

    /**
     * Reserve inventory quantity (e.g., when order is placed but not paid)
     *
     * @param quantityToReserve Amount to reserve
     * @throws BadRequestException if insufficient available stock
     */
    public void reserveQuantity(Integer quantityToReserve) {
        if (quantityToReserve == null || quantityToReserve <= 0) {
            throw new BadRequestException("Reserve quantity must be positive");
        }

        if (getAvailableStock() < quantityToReserve) {
            throw new BadRequestException(
                    String.format("Insufficient available stock. Available: %d, Requested: %d",
                            getAvailableStock(), quantityToReserve)
            );
        }

        this.reservedQuantity += quantityToReserve;
    }

    /**
     * Release reserved inventory (e.g., when payment fails or order is cancelled)
     *
     * @param quantityToRelease Amount to release from reserved
     */
    public void releaseReservedQuantity(Integer quantityToRelease) {
        if (quantityToRelease == null || quantityToRelease <= 0) {
            return; // Silent return for invalid inputs during cleanup
        }

        // Reduce reserved quantity, but don't go below 0
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantityToRelease);
    }

    /**
     * Add inventory quantity (e.g., restocking)
     *
     * @param quantityToAdd Amount to add to inventory
     */
    public void addQuantity(Integer quantityToAdd) {
        if (quantityToAdd == null || quantityToAdd <= 0) {
            throw new BadRequestException("Add quantity must be positive");
        }

        this.quantity += quantityToAdd;
    }
}