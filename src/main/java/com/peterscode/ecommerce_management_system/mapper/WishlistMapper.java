package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.response.RecentlyViewedResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.WishlistResponse;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import com.peterscode.ecommerce_management_system.model.entity.RecentlyViewedProduct;
import com.peterscode.ecommerce_management_system.model.entity.Wishlist;
import org.springframework.stereotype.Component;


@Component
public class WishlistMapper {

    public WishlistResponse toResponse(Wishlist wishlist) {
        if (wishlist == null) return null;

        Product product = wishlist.getProduct();

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSku(product.getSku())
                .productImageUrl(product.getImageUrl())
                .productBrand(product.getBrand())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .discountPercentage(product.getDiscountPercentage())
                .isInStock(product.isInStock())
                .isActive(product.getIsActive())
                .availableStock(product.getStockQuantity())
                .addedAt(wishlist.getCreatedAt())
                .build();
    }

    public RecentlyViewedResponse toRecentlyViewedResponse(RecentlyViewedProduct rv) {
        if (rv == null) return null;

        Product product = rv.getProduct();

        return RecentlyViewedResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productSku(product.getSku())
                .productImageUrl(product.getImageUrl())
                .productBrand(product.getBrand())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .discountPercentage(product.getDiscountPercentage())
                .isInStock(product.isInStock())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .viewedAt(rv.getViewedAt())
                .build();
    }
}

