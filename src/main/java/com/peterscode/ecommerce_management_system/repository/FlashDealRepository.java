package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.FlashDeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashDealRepository extends JpaRepository<FlashDeal, Long> {

    @Query("SELECT fd FROM FlashDeal fd WHERE fd.isActive = true " +
            "AND fd.startsAt <= :now AND fd.endsAt >= :now")
    List<FlashDeal> findActiveDeals(@Param("now") LocalDateTime now);

    @Query("SELECT fd FROM FlashDeal fd WHERE fd.isActive = true " +
            "AND fd.startsAt <= :now AND fd.endsAt >= :now")
    Page<FlashDeal> findActiveDeals(@Param("now") LocalDateTime now, Pageable pageable);

    Optional<FlashDeal> findByProductIdAndIsActiveTrue(Long productId);

    List<FlashDeal> findByProductId(Long productId);

    @Query("SELECT fd FROM FlashDeal fd WHERE fd.isActive = true " +
            "AND fd.startsAt > :now ORDER BY fd.startsAt ASC")
    List<FlashDeal> findUpcomingDeals(@Param("now") LocalDateTime now);
}

