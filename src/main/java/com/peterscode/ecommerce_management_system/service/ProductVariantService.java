package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.request.ProductVariantRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ProductVariantResponse;

import java.util.List;

public interface ProductVariantService {

    ProductVariantResponse createVariant(Long productId, ProductVariantRequest request);

    ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request);

    ProductVariantResponse getVariantById(Long variantId);

    List<ProductVariantResponse> getVariantsByProductId(Long productId);

    List<ProductVariantResponse> getActiveVariantsByProductId(Long productId);

    void deleteVariant(Long variantId);

    void deactivateVariant(Long variantId);
}

