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
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString(exclude = {"user", "items"})
@Table(name = "carts", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_session_id", columnList = "session_id")
})
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "session_id", length = 100)
    private String sessionId; // For guest users

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "total_items")
    @Builder.Default
    private Integer totalItems = 0;

    @Column(name = "subtotal", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Guest cart expiry

    @Column(name = "estimated_shipping_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal estimatedShippingCost = BigDecimal.ZERO;

    @Column(name = "selected_items_total", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal selectedItemsTotal = BigDecimal.ZERO;

    @Column(name = "total_savings", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalSavings = BigDecimal.ZERO;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business logic methods
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
        recalculateTotals();
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
        recalculateTotals();
    }

    public void clearItems() {
        items.clear();
        recalculateTotals();
    }

    public void recalculateTotals() {
        // Only count items that are NOT saved for later
        List<CartItem> activeItems = items.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getSavedForLater()))
                .toList();

        this.totalItems = activeItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        this.subtotal = activeItems.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Selected items total (Kilimall checkout calculation)
        this.selectedItemsTotal = activeItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getSelected()))
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate savings (original price vs discount price)
        this.totalSavings = activeItems.stream()
                .filter(item -> item.getPriceAtAddition() != null && item.getUnitPrice() != null)
                .map(item -> {
                    BigDecimal originalTotal = item.getPriceAtAddition().multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal currentTotal = item.getTotalPrice();
                    BigDecimal saving = originalTotal.subtract(currentTotal);
                    return saving.compareTo(BigDecimal.ZERO) > 0 ? saving : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total (subtotal - discount + tax + shipping)
        this.totalAmount = selectedItemsTotal
                .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                .add(taxAmount != null ? taxAmount : BigDecimal.ZERO)
                .add(estimatedShippingCost != null ? estimatedShippingCost : BigDecimal.ZERO);
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}