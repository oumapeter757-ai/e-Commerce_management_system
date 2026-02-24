package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.RecentlyViewedProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecentlyViewedRepository extends JpaRepository<RecentlyViewedProduct, Long> {

    @Query("SELECT rv FROM RecentlyViewedProduct rv WHERE rv.user.id = :userId ORDER BY rv.viewedAt DESC")
    List<RecentlyViewedProduct> findByUserIdOrderByViewedAtDesc(@Param("userId") Long userId, Pageable pageable);

    Optional<RecentlyViewedProduct> findByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT COUNT(rv) FROM RecentlyViewedProduct rv WHERE rv.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RecentlyViewedProduct rv WHERE rv.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RecentlyViewedProduct rv WHERE rv.viewedAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}

