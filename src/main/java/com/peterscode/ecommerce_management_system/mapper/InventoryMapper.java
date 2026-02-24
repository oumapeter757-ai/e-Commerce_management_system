package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.response.InventoryResponse;
import com.peterscode.ecommerce_management_system.model.entity.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        // Calculate total quantity
        Integer totalQuantity = inventory.getAvailableStock() + inventory.getReservedStock();

        // Determine status
        String status = "IN_STOCK";
        if (inventory.getAvailableStock() == 0) {
            status = "OUT_OF_STOCK";
        } else if (inventory.getAvailableStock() <= inventory.getLowStockThreshold()) {
            status = "LOW_STOCK";
        }

        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .productSku(inventory.getProduct().getSku())
                .totalQuantity(totalQuantity)
                .reservedQuantity(inventory.getReservedStock())
                .availableStock(inventory.getAvailableStock())
                .lowStockThreshold(inventory.getLowStockThreshold())
                .status(status)
                .lastUpdated(inventory.getUpdatedAt())
                .build();
    }
}