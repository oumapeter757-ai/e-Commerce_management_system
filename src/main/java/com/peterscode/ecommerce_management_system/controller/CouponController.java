package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.request.CouponRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.CouponResponse;
import com.peterscode.ecommerce_management_system.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupon Management", description = "APIs for coupon/promotion management")
public class CouponController {

    private final CouponService couponService;

    // ==================== PUBLIC ENDPOINTS ====================

    @GetMapping("/validate")
    @Operation(summary = "Validate a coupon code (Public/Customer)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal,
            Authentication authentication) {

        Long userId = authentication != null ? getUserId(authentication) : null;
        BigDecimal discount = couponService.validateAndApplyCoupon(code, orderTotal, userId);
        boolean isFreeShipping = couponService.isFreeShippingCoupon(code);

        Map<String, Object> data = Map.of(
                "code", code,
                "discount", discount,
                "freeShipping", isFreeShipping,
                "valid", true
        );

        return ResponseEntity.ok(ApiResponse.success("Coupon is valid", data));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new coupon (Admin)")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CouponRequest request) {
        CouponResponse coupon = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupon created successfully", coupon));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all coupons (Admin)")
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> getAllCoupons(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CouponResponse> coupons = couponService.getAllCoupons(pageable);
        return ResponseEntity.ok(ApiResponse.success("Coupons retrieved", coupons));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get active coupons (Admin)")
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> getActiveCoupons(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CouponResponse> coupons = couponService.getActiveCoupons(pageable);
        return ResponseEntity.ok(ApiResponse.success("Active coupons retrieved", coupons));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get coupon by ID (Admin)")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(@PathVariable Long id) {
        CouponResponse coupon = couponService.getCouponById(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon retrieved", coupon));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update coupon (Admin)")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request) {
        CouponResponse coupon = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(ApiResponse.success("Coupon updated", coupon));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate coupon (Admin)")
    public ResponseEntity<ApiResponse<Void>> deactivateCoupon(@PathVariable Long id) {
        couponService.deactivateCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon deactivated"));
    }

    // ==================== HELPER ====================

    private Long getUserId(Authentication authentication) {
        if (authentication == null) return null;
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

