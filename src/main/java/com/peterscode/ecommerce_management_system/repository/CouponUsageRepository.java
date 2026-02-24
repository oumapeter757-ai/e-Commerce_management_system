package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    @Query("SELECT COUNT(cu) FROM CouponUsage cu WHERE cu.coupon.id = :couponId AND cu.user.id = :userId")
    long countByCouponIdAndUserId(@Param("couponId") Long couponId, @Param("userId") Long userId);

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);
}

