package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    Optional<Coupon> findByCodeAndIsActiveTrue(String code);

    boolean existsByCode(String code);

    Page<Coupon> findByIsActiveTrue(Pageable pageable);
}

