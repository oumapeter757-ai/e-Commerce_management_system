package com.peterscode.ecommerce_management_system.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer totalQuantity;
    private Integer reservedQuantity;
    private Integer availableStock;
    private Integer lowStockThreshold;
    private String status; // "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"
    private LocalDateTime lastUpdated;
}