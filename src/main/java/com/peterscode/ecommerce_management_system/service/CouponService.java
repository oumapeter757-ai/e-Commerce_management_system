package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.request.CouponRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.CouponResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface CouponService {

    CouponResponse createCoupon(CouponRequest request);

    CouponResponse getCouponById(Long id);

    CouponResponse getCouponByCode(String code);

    Page<CouponResponse> getAllCoupons(Pageable pageable);

    Page<CouponResponse> getActiveCoupons(Pageable pageable);

    CouponResponse updateCoupon(Long id, CouponRequest request);

    void deactivateCoupon(Long id);

    /**
     * Validate and calculate discount for a coupon code.
     * @return discount amount (BigDecimal.ZERO if invalid)
     */
    BigDecimal validateAndApplyCoupon(String code, BigDecimal orderTotal, Long userId);

    /**
     * Record coupon usage after successful order.
     */
    void recordCouponUsage(String code, Long userId, Long orderId);

    /**
     * Check if coupon grants free shipping.
     */
    boolean isFreeShippingCoupon(String code);
}

