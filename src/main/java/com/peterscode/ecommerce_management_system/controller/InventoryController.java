package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.request.InventoryUpdateRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;

import com.peterscode.ecommerce_management_system.model.dto.response.InventoryResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import com.peterscode.ecommerce_management_system.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "APIs for managing product stock")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory for product")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(@PathVariable Long productId) {
        InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success(String.valueOf(response)));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Update absolute stock quantity")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryUpdateRequest request) {
        InventoryResponse response = inventoryService.updateStock(productId, request);
        return ResponseEntity.ok(ApiResponse.success(String.valueOf(response)));
    }

    @PostMapping("/{productId}/restock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Add stock to existing inventory")
    public ResponseEntity<ApiResponse<String>> restock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        inventoryService.restock(productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock added successfully"));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Get low stock products")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getLowStock(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<InventoryResponse> response = inventoryService.getLowStockInventory(pageable);
        return ResponseEntity.ok(ApiResponse.success(String.valueOf(response)));
    }
}