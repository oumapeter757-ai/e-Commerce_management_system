package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.request.ProductVariantRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ProductVariantResponse;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import com.peterscode.ecommerce_management_system.model.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductVariantMapper {

    public ProductVariantResponse toResponse(ProductVariant variant) {
        if (variant == null) return null;

        Product product = variant.getProduct();

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .sku(variant.getSku())
                .variantName(variant.getVariantName())
                .color(variant.getColor())
                .size(variant.getSize())
                .material(variant.getMaterial())
                .priceAdjustment(variant.getPriceAdjustment())
                .effectivePrice(variant.getEffectivePrice())
                .stockQuantity(variant.getStockQuantity())
                .isInStock(variant.isInStock())
                .imageUrl(variant.getImageUrl())
                .isActive(variant.getIsActive())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }

    public ProductVariant toEntity(ProductVariantRequest request, Product product) {
        if (request == null) return null;

        return ProductVariant.builder()
                .product(product)
                .sku(request.getSku())
                .variantName(request.getVariantName())
                .color(request.getColor())
                .size(request.getSize())
                .material(request.getMaterial())
                .priceAdjustment(request.getPriceAdjustment() != null ? request.getPriceAdjustment() : BigDecimal.ZERO)
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .isActive(true)
                .build();
    }
}

