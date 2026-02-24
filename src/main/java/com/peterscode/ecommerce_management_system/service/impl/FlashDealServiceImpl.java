package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.FlashDealMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.FlashDealRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.FlashDealResponse;
import com.peterscode.ecommerce_management_system.model.entity.FlashDeal;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import com.peterscode.ecommerce_management_system.repository.FlashDealRepository;
import com.peterscode.ecommerce_management_system.repository.ProductRepository;
import com.peterscode.ecommerce_management_system.service.FlashDealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashDealServiceImpl implements FlashDealService {

    private final FlashDealRepository flashDealRepository;
    private final ProductRepository productRepository;
    private final FlashDealMapper flashDealMapper;

    @Override
    @Transactional
    public FlashDealResponse createDeal(FlashDealRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (request.getEndsAt().isBefore(request.getStartsAt())) {
            throw new BadRequestException("End date must be after start date");
        }

        if (request.getDealPrice().compareTo(product.getPrice()) >= 0) {
            throw new BadRequestException("Deal price must be lower than original price");
        }

        FlashDeal deal = FlashDeal.builder()
                .product(product)
                .dealName(request.getDealName())
                .dealPrice(request.getDealPrice())
                .originalPrice(product.getPrice())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .stockLimit(request.getStockLimit())
                .isActive(true)
                .build();

        deal.setDiscountPercentage(deal.getCalculatedDiscountPercentage());

        FlashDeal saved = flashDealRepository.save(deal);
        log.info("Flash deal created for product {}: {} ({}% off)",
                product.getId(), deal.getDealName(), deal.getDiscountPercentage());
        return flashDealMapper.toResponse(saved);
    }

    @Override
    public FlashDealResponse getDealById(Long dealId) {
        FlashDeal deal = flashDealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Flash deal not found"));
        return flashDealMapper.toResponse(deal);
    }

    @Override
    public List<FlashDealResponse> getActiveDeals() {
        return flashDealRepository.findActiveDeals(LocalDateTime.now())
                .stream().map(flashDealMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public Page<FlashDealResponse> getActiveDeals(Pageable pageable) {
        return flashDealRepository.findActiveDeals(LocalDateTime.now(), pageable)
                .map(flashDealMapper::toResponse);
    }

    @Override
    public List<FlashDealResponse> getUpcomingDeals() {
        return flashDealRepository.findUpcomingDeals(LocalDateTime.now())
                .stream().map(flashDealMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public FlashDealResponse getDealByProductId(Long productId) {
        FlashDeal deal = flashDealRepository.findByProductIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No active flash deal for this product"));
        return flashDealMapper.toResponse(deal);
    }

    @Override
    @Transactional
    public FlashDealResponse updateDeal(Long dealId, FlashDealRequest request) {
        FlashDeal deal = flashDealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Flash deal not found"));

        deal.setDealName(request.getDealName());
        deal.setDealPrice(request.getDealPrice());
        deal.setStartsAt(request.getStartsAt());
        deal.setEndsAt(request.getEndsAt());
        deal.setStockLimit(request.getStockLimit());
        deal.setDiscountPercentage(deal.getCalculatedDiscountPercentage());

        return flashDealMapper.toResponse(flashDealRepository.save(deal));
    }

    @Override
    @Transactional
    public void deactivateDeal(Long dealId) {
        FlashDeal deal = flashDealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Flash deal not found"));
        deal.setIsActive(false);
        flashDealRepository.save(deal);
        log.info("Flash deal {} deactivated", dealId);
    }

    @Override
    @Transactional
    public void incrementSoldCount(Long dealId) {
        FlashDeal deal = flashDealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Flash deal not found"));
        deal.incrementSoldCount();
        flashDealRepository.save(deal);
    }
}

