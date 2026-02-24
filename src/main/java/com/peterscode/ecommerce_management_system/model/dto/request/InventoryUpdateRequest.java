package com.peterscode.ecommerce_management_system.model.dto.request;

import lombok.Data;

@Data
public class InventoryUpdateRequest {
    private Integer availableStock;
    private Integer reservedStock;
    private Integer lowStockThreshold;
    private Integer restockQuantity;
}