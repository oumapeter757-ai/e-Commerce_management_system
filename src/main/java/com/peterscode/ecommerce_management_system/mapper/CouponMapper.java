package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.request.CouponRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.CouponResponse;
import com.peterscode.ecommerce_management_system.model.entity.Coupon;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public CouponResponse toResponse(Coupon coupon) {
        if (coupon == null) return null;

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .usageCount(coupon.getUsageCount())
                .perUserLimit(coupon.getPerUserLimit())
                .startsAt(coupon.getStartsAt())
                .expiresAt(coupon.getExpiresAt())
                .isActive(coupon.getIsActive())
                .isValid(coupon.isValid())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }

    public Coupon toEntity(CouponRequest request) {
        if (request == null) return null;

        return Coupon.builder()
                .code(request.getCode().toUpperCase().trim())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .perUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1)
                .startsAt(request.getStartsAt())
                .expiresAt(request.getExpiresAt())
                .isActive(true)
                .build();
    }
}

