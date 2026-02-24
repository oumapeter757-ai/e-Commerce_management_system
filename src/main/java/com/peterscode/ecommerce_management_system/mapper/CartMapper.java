package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.common.CartItemDTO;
import com.peterscode.ecommerce_management_system.model.dto.response.CartResponse;
import com.peterscode.ecommerce_management_system.model.entity.Cart;
import com.peterscode.ecommerce_management_system.model.entity.CartItem;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return null;
        }

        List<CartItemDTO> activeItems = Collections.emptyList();
        List<CartItemDTO> savedForLaterItems = Collections.emptyList();
        List<CartItemDTO> priceChangedItems = Collections.emptyList();

        if (cart.getItems() != null) {
            // Active cart items (not saved for later)
            activeItems = cart.getItems().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getSavedForLater()))
                    .map(this::toCartItemDTO)
                    .collect(Collectors.toList());

            // Saved for later items
            savedForLaterItems = cart.getItems().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getSavedForLater()))
                    .map(this::toCartItemDTO)
                    .collect(Collectors.toList());

            // Items with price changes (alerts)
            priceChangedItems = activeItems.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getPriceChanged()))
                    .collect(Collectors.toList());
        }

        int selectedItemCount = activeItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getSelected()))
                .mapToInt(CartItemDTO::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .sessionId(cart.getSessionId())
                .items(activeItems)
                .savedForLaterItems(savedForLaterItems)
                .totalItems(cart.getTotalItems())
                .selectedItemCount(selectedItemCount)
                .subtotal(cart.getSubtotal())
                .selectedItemsTotal(cart.getSelectedItemsTotal())
                .discountAmount(cart.getDiscountAmount())
                .taxAmount(cart.getTaxAmount())
                .estimatedShippingCost(cart.getEstimatedShippingCost())
                .totalAmount(cart.getTotalAmount())
                .totalSavings(cart.getTotalSavings())
                .couponCode(cart.getCouponCode())
                .priceChangedItems(priceChangedItems)
                .expiresAt(cart.getExpiresAt())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    public CartItemDTO toCartItemDTO(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }

        Product product = cartItem.getProduct();
        BigDecimal currentPrice = product != null ? product.getActualPrice() : null;
        BigDecimal priceAtAddition = cartItem.getPriceAtAddition();

        // Detect price change
        boolean priceChanged = false;
        BigDecimal priceDifference = BigDecimal.ZERO;
        if (priceAtAddition != null && currentPrice != null) {
            priceDifference = currentPrice.subtract(priceAtAddition);
            priceChanged = priceDifference.compareTo(BigDecimal.ZERO) != 0;
        }

        // Compute discount percentage
        BigDecimal discountPercentage = BigDecimal.ZERO;
        if (product != null && product.getDiscountPrice() != null
                && product.getPrice() != null
                && product.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            discountPercentage = product.getPrice().subtract(product.getDiscountPrice())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(product.getPrice(), 2, RoundingMode.HALF_UP);
        }

        return CartItemDTO.builder()
                .id(cartItem.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .productSku(product != null ? product.getSku() : null)
                .productImageUrl(product != null ? product.getImageUrl() : null)
                .productBrand(product != null ? product.getBrand() : null)
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .totalPrice(cartItem.getTotalPrice())
                .availableStock(product != null ? product.getStockQuantity() : 0)
                .isAvailable(product != null && Boolean.TRUE.equals(product.getIsActive()) && product.isInStock())
                .selected(cartItem.getSelected())
                .savedForLater(cartItem.getSavedForLater())
                .priceAtAddition(priceAtAddition)
                .currentPrice(currentPrice)
                .priceChanged(priceChanged)
                .priceDifference(priceDifference)
                .notes(cartItem.getNotes())
                .maxBuyQuantity(cartItem.getMaxBuyQuantity() != null
                        ? cartItem.getMaxBuyQuantity()
                        : (product != null ? product.getMaxBuyQuantity() : null))
                .discountPercentage(discountPercentage)
                .createdAt(cartItem.getCreatedAt())
                .updatedAt(cartItem.getUpdatedAt())
                .build();
    }
}