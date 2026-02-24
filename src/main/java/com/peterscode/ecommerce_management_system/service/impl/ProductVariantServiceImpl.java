package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.ProductVariantMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.ProductVariantRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ProductVariantResponse;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import com.peterscode.ecommerce_management_system.model.entity.ProductVariant;
import com.peterscode.ecommerce_management_system.repository.ProductRepository;
import com.peterscode.ecommerce_management_system.repository.ProductVariantRepository;
import com.peterscode.ecommerce_management_system.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper variantMapper;

    @Override
    @Transactional
    public ProductVariantResponse createVariant(Long productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (variantRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Variant SKU already exists: " + request.getSku());
        }

        ProductVariant variant = variantMapper.toEntity(request, product);
        ProductVariant saved = variantRepository.save(variant);
        log.info("Created variant {} for product {}", saved.getSku(), productId);
        return variantMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        variant.setVariantName(request.getVariantName());
        variant.setColor(request.getColor());
        variant.setSize(request.getSize());
        variant.setMaterial(request.getMaterial());
        variant.setPriceAdjustment(request.getPriceAdjustment());
        variant.setStockQuantity(request.getStockQuantity());
        variant.setImageUrl(request.getImageUrl());

        return variantMapper.toResponse(variantRepository.save(variant));
    }

    @Override
    public ProductVariantResponse getVariantById(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        return variantMapper.toResponse(variant);
    }

    @Override
    public List<ProductVariantResponse> getVariantsByProductId(Long productId) {
        return variantRepository.findByProductId(productId)
                .stream().map(variantMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ProductVariantResponse> getActiveVariantsByProductId(Long productId) {
        return variantRepository.findByProductIdAndIsActiveTrue(productId)
                .stream().map(variantMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        variantRepository.delete(variant);
        log.info("Deleted variant: {}", variant.getSku());
    }

    @Override
    @Transactional
    public void deactivateVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        variant.setIsActive(false);
        variantRepository.save(variant);
        log.info("Deactivated variant: {}", variant.getSku());
    }
}

