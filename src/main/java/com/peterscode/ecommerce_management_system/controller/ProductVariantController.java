package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.request.ProductVariantRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.ProductVariantResponse;
import com.peterscode.ecommerce_management_system.service.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/products/{productId}/variants")
@RequiredArgsConstructor
@Tag(name = "Product Variant Management", description = "APIs for managing product color/size/material variants")
public class ProductVariantController {

    private final ProductVariantService variantService;

    // ==================== PUBLIC ENDPOINTS ====================

    @GetMapping
    @Operation(summary = "Get all active variants for a product (Public)")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariants(
            @PathVariable Long productId) {
        List<ProductVariantResponse> variants = variantService.getActiveVariantsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("Variants retrieved", variants));
    }

    @GetMapping("/{variantId}")
    @Operation(summary = "Get variant by ID (Public)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        ProductVariantResponse variant = variantService.getVariantById(variantId);
        return ResponseEntity.ok(ApiResponse.success("Variant retrieved", variant));
    }

    // ==================== ADMIN/SELLER ENDPOINTS ====================

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Get all variants including inactive (Admin/Seller)")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getAllVariants(
            @PathVariable Long productId) {
        List<ProductVariantResponse> variants = variantService.getVariantsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("All variants retrieved", variants));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Create a product variant (Admin/Seller)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse variant = variantService.createVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Variant created", variant));
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Update a product variant (Admin/Seller)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse variant = variantService.updateVariant(variantId, request);
        return ResponseEntity.ok(ApiResponse.success("Variant updated", variant));
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Deactivate a product variant (Admin/Seller)")
    public ResponseEntity<ApiResponse<Void>> deactivateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        variantService.deactivateVariant(variantId);
        return ResponseEntity.ok(ApiResponse.success("Variant deactivated"));
    }
}

