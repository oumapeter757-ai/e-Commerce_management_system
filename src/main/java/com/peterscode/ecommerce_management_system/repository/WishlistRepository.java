package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserId(Long userId);

    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT w.product.id FROM Wishlist w WHERE w.user.id = :userId")
    List<Long> findProductIdsByUserId(@Param("userId") Long userId);
}

