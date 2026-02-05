package com.peterscode.ecommerce_management_system.model.entity;

import com.peterscode.ecommerce_management_system.model.enums.ShippingStatus;
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
@Table(name = "shipping", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_tracking_number", columnList = "tracking_number"),
        @Index(name = "idx_status", columnList = "status")
})
public class Shipping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "tracking_number", unique = true, length = 100)
    private String trackingNumber;

    @Column(nullable = false, length = 100)
    private String carrier; // FedEx, UPS, DHL, USPS, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShippingStatus status;

    @Column(name = "shipping_method", length = 50)
    private String shippingMethod; // Standard, Express, Overnight, etc.

    @Column(name = "shipping_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingCost;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shipping_address_id", nullable = false)
    private Address shippingAddress;

    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight; // in kg

    @Column(length = 50)
    private String dimensions; // e.g., "30x20x15 cm"

    @Column(name = "package_count")
    @Builder.Default
    private Integer packageCount = 1;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    @Column(name = "delivery_instructions", columnDefinition = "TEXT")
    private String deliveryInstructions;

    @Column(name = "signature_required")
    @Builder.Default
    private Boolean signatureRequired = false;

    @Column(name = "insurance_amount", precision = 10, scale = 2)
    private BigDecimal insuranceAmount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "exception_reason", columnDefinition = "TEXT")
    private String exceptionReason; // For delivery exceptions

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business logic methods
    public void markAsShipped() {
        this.status = ShippingStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
    }

    public void markAsInTransit() {
        this.status = ShippingStatus.IN_TRANSIT;
    }

    public void markAsOutForDelivery() {
        this.status = ShippingStatus.OUT_FOR_DELIVERY;
    }

    public void markAsDelivered() {
        this.status = ShippingStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
        this.actualDeliveryDate = LocalDateTime.now();
    }

    public void markAsException(String reason) {
        this.status = ShippingStatus.EXCEPTION;
        this.exceptionReason = reason;
    }

    public void markAsReturned() {
        this.status = ShippingStatus.RETURNED;
    }

    public boolean isDelivered() {
        return status == ShippingStatus.DELIVERED;
    }

    public boolean isInTransit() {
        return status == ShippingStatus.IN_TRANSIT ||
                status == ShippingStatus.OUT_FOR_DELIVERY;
    }

    public boolean hasException() {
        return status == ShippingStatus.EXCEPTION;
    }
}