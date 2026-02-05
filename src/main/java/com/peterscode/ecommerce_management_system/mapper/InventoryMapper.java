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

        String status = "IN_STOCK";
        if (inventory.getAvailableStock() == 0) {
            status = "OUT_OF_STOCK";
        } else if (inventory.isLowStock()) {
            status = "LOW_STOCK";
        }

        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .productSku(inventory.getProduct().getSku())
                .totalQuantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableStock(inventory.getAvailableStock())
                .lowStockThreshold(inventory.getLowStockThreshold())
                .status(status)
                .lastUpdated(inventory.getLastUpdated())
                .build();
    }
}