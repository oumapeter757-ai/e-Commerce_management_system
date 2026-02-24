package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.CouponMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.CouponRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.CouponResponse;
import com.peterscode.ecommerce_management_system.model.entity.Coupon;
import com.peterscode.ecommerce_management_system.model.entity.CouponUsage;
import com.peterscode.ecommerce_management_system.model.entity.Order;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.model.enums.DiscountType;
import com.peterscode.ecommerce_management_system.repository.CouponRepository;
import com.peterscode.ecommerce_management_system.repository.CouponUsageRepository;
import com.peterscode.ecommerce_management_system.repository.OrderRepository;
import com.peterscode.ecommerce_management_system.repository.UserRepository;
import com.peterscode.ecommerce_management_system.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CouponMapper couponMapper;

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode().toUpperCase().trim())) {
            throw new BadRequestException("Coupon code already exists");
        }

        if (request.getExpiresAt().isBefore(request.getStartsAt())) {
            throw new BadRequestException("Expiry date must be after start date");
        }

        Coupon coupon = couponMapper.toEntity(request);
        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon created: {}", saved.getCode());
        return couponMapper.toResponse(saved);
    }

    @Override
    public CouponResponse getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        return couponMapper.toResponse(coupon);
    }

    @Override
    public CouponResponse getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        return couponMapper.toResponse(coupon);
    }

    @Override
    public Page<CouponResponse> getAllCoupons(Pageable pageable) {
        return couponRepository.findAll(pageable).map(couponMapper::toResponse);
    }

    @Override
    public Page<CouponResponse> getActiveCoupons(Pageable pageable) {
        return couponRepository.findByIsActiveTrue(pageable).map(couponMapper::toResponse);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setPerUserLimit(request.getPerUserLimit());
        coupon.setStartsAt(request.getStartsAt());
        coupon.setExpiresAt(request.getExpiresAt());

        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public void deactivateCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        coupon.setIsActive(false);
        couponRepository.save(coupon);
        log.info("Coupon deactivated: {}", coupon.getCode());
    }

    @Override
    public BigDecimal validateAndApplyCoupon(String code, BigDecimal orderTotal, Long userId) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(code.toUpperCase().trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired coupon code"));

        // Validate coupon is still valid
        if (!coupon.isValid()) {
            throw new BadRequestException("Coupon is no longer valid");
        }

        // Check minimum order amount
        if (!coupon.meetsMinimumOrder(orderTotal)) {
            throw new BadRequestException(
                    "Minimum order amount of KES " + coupon.getMinOrderAmount() + " required for this coupon");
        }

        // Check per-user usage limit
        if (userId != null && coupon.getPerUserLimit() != null) {
            long userUsageCount = couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId);
            if (userUsageCount >= coupon.getPerUserLimit()) {
                throw new BadRequestException("You have already used this coupon the maximum number of times");
            }
        }

        BigDecimal discount = coupon.calculateDiscount(orderTotal);
        log.info("Coupon {} validated for user {}. Discount: KES {}", code, userId, discount);
        return discount;
    }

    @Override
    @Transactional
    public void recordCouponUsage(String code, Long userId, Long orderId) {
        if (code == null || code.isBlank()) return;

        Coupon coupon = couponRepository.findByCode(code.toUpperCase().trim()).orElse(null);
        if (coupon == null) return;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderId != null
                ? orderRepository.findById(orderId).orElse(null)
                : null;

        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .user(user)
                .order(order)
                .build();

        couponUsageRepository.save(usage);
        coupon.incrementUsage();
        couponRepository.save(coupon);

        log.info("Coupon usage recorded: {} by user {} for order {}", code, userId, orderId);
    }

    @Override
    public boolean isFreeShippingCoupon(String code) {
        if (code == null || code.isBlank()) return false;
        return couponRepository.findByCodeAndIsActiveTrue(code.toUpperCase().trim())
                .map(c -> c.getDiscountType() == DiscountType.FREE_SHIPPING && c.isValid())
                .orElse(false);
    }
}

