package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.response.FlashDealResponse;
import com.peterscode.ecommerce_management_system.model.entity.FlashDeal;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class FlashDealMapper {

    public FlashDealResponse toResponse(FlashDeal deal) {
        if (deal == null) return null;

        Product product = deal.getProduct();

        return FlashDealResponse.builder()
                .id(deal.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .productImageUrl(product != null ? product.getImageUrl() : null)
                .dealName(deal.getDealName())
                .dealPrice(deal.getDealPrice())
                .originalPrice(deal.getOriginalPrice())
                .discountPercentage(deal.getCalculatedDiscountPercentage())
                .startsAt(deal.getStartsAt())
                .endsAt(deal.getEndsAt())
                .remainingTimeInSeconds(deal.getRemainingTimeInSeconds())
                .stockLimit(deal.getStockLimit())
                .soldCount(deal.getSoldCount())
                .remainingStock(deal.getRemainingStock())
                .isActive(deal.getIsActive())
                .isCurrentlyActive(deal.isCurrentlyActive())
                .createdAt(deal.getCreatedAt())
                .updatedAt(deal.getUpdatedAt())
                .build();
    }
}

