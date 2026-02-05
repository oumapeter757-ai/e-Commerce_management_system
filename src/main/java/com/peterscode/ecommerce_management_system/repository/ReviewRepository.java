package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductId(Long productId, Pageable pageable);

    Page<Review> findByProductIdAndIsApprovedTrue(Long productId, Pageable pageable);

    Page<Review> findByUserId(Long userId, Pageable pageable);

    boolean existsByProductIdAndUserId(Long productId, Long userId);


    List<Review> findByProductIdAndUserId(Long productId, Long userId);

    Optional<Review> findByOrderIdAndProductIdAndUserId(Long orderId, Long productId, Long userId);

    boolean existsByOrderIdAndProductIdAndUserId(Long orderId, Long productId, Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Long countApprovedReviewsByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.rating = :rating AND r.isApproved = true")
    Long countByProductIdAndRating(@Param("productId") Long productId, @Param("rating") Integer rating);

    Page<Review> findByIsApprovedFalse(Pageable pageable);

    Page<Review> findByVerifiedPurchaseTrue(Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.rating >= :minRating AND r.isApproved = true")
    Page<Review> findHighRatedReviews(@Param("productId") Long productId,
                                      @Param("minRating") Integer minRating,
                                      Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    void deleteByProductId(Long productId);

    void deleteByUserId(Long userId);
}