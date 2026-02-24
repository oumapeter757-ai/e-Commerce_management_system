package com.peterscode.ecommerce_management_system.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString(exclude = {"user", "product"})
@Table(name = "wishlists", indexes = {
        @Index(name = "idx_wishlist_user_id", columnList = "user_id"),
        @Index(name = "idx_wishlist_product_id", columnList = "product_id"),
        @Index(name = "idx_wishlist_created_at", columnList = "created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_wishlist_user_product", columnNames = {"user_id", "product_id"})
})
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

