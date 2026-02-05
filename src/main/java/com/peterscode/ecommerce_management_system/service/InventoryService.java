package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.request.InventoryUpdateRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.InventoryResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    InventoryResponse getInventoryByProductId(Long productId);

    void restock(Long productId, Integer quantity);

    InventoryResponse updateStock(Long productId, InventoryUpdateRequest request);

    void reserveStock(Long productId, Integer quantity);

    void releaseReservedStock(Long productId, Integer quantity);

    void confirmStockReduction(Long productId, Integer quantity);

    Integer getStock(Long productId);

    PageResponse<InventoryResponse> getLowStockInventory(Pageable pageable);
}