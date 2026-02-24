package com.peterscode.ecommerce_management_system.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString(exclude = {"user", "product"})
@Table(name = "recently_viewed_products", indexes = {
        @Index(name = "idx_rv_user_id", columnList = "user_id"),
        @Index(name = "idx_rv_product_id", columnList = "product_id"),
        @Index(name = "idx_rv_viewed_at", columnList = "viewed_at"),
        @Index(name = "idx_rv_user_viewed", columnList = "user_id, viewed_at")
})
public class RecentlyViewedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) {
            viewedAt = LocalDateTime.now();
        }
    }
}

