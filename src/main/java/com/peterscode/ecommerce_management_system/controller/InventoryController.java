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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Inventory Controller - Clean API layer without business logic
 * All business logic is handled in InventoryService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "APIs for managing product stock levels")
public class InventoryController {

    private final InventoryService inventoryService;

    // ==================== INVENTORY QUERIES ====================

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory details for product")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(
            @PathVariable Long productId) {

        log.debug("Fetching inventory for product: {}", productId);

        InventoryResponse response = inventoryService.getInventoryByProductId(productId);

        return ResponseEntity.ok(ApiResponse.success("Inventory retrieved successfully", response));
    }

    @GetMapping("/{productId}/available")
    @Operation(summary = "Get available stock quantity")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getAvailableStock(
            @PathVariable Long productId) {

        Integer availableStock = inventoryService.getAvailableStock(productId);

        Map<String, Integer> result = new HashMap<>();
        result.put("productId", productId.intValue());
        result.put("availableStock", availableStock);

        return ResponseEntity.ok(ApiResponse.success("Available stock retrieved", result));
    }

    @GetMapping("/{productId}/total")
    @Operation(summary = "Get total stock (available + reserved)")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getTotalStock(
            @PathVariable Long productId) {

        Integer totalStock = inventoryService.getTotalStock(productId);

        Map<String, Integer> result = new HashMap<>();
        result.put("productId", productId.intValue());
        result.put("totalStock", totalStock);

        return ResponseEntity.ok(ApiResponse.success("Total stock retrieved", result));
    }

    @GetMapping("/{productId}/reserved")
    @Operation(summary = "Get reserved stock quantity")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getReservedStock(
            @PathVariable Long productId) {

        Integer reservedStock = inventoryService.getReservedStock(productId);

        Map<String, Integer> result = new HashMap<>();
        result.put("productId", productId.intValue());
        result.put("reservedStock", reservedStock);

        return ResponseEntity.ok(ApiResponse.success("Reserved stock retrieved", result));
    }

    @GetMapping("/{productId}/check-availability")
    @Operation(summary = "Check if sufficient stock is available")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkStockAvailability(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {

        boolean isAvailable = inventoryService.isStockAvailable(productId, quantity);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("requestedQuantity", quantity);
        result.put("isAvailable", isAvailable);

        return ResponseEntity.ok(ApiResponse.success("Stock availability checked", result));
    }

    @GetMapping("/{productId}/low-stock")
    @Operation(summary = "Check if product is low on stock")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkLowStock(
            @PathVariable Long productId) {

        boolean isLowStock = inventoryService.isLowStock(productId);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("isLowStock", isLowStock);

        return ResponseEntity.ok(ApiResponse.success("Low stock status retrieved", result));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Get all products with low stock levels")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getLowStockProducts(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Fetching low stock inventory - Page: {}, Size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        PageResponse<InventoryResponse> response = inventoryService.getLowStockInventory(pageable);

        return ResponseEntity.ok(ApiResponse.success(
                "Low stock products retrieved (" + response.getTotalElements() + " items)", response));
    }

    // ==================== INVENTORY UPDATES ====================

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Update inventory details (absolute values)")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryUpdateRequest request) {

        log.info("Updating inventory for product: {} - Request: {}", productId, request);

        InventoryResponse response = inventoryService.updateStock(productId, request);

        return ResponseEntity.ok(ApiResponse.success("Inventory updated successfully", response));
    }

    @PostMapping("/{productId}/restock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Add stock to existing inventory (incremental)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> restock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {

        log.info("Restocking product: {} with {} units", productId, quantity);

        inventoryService.restock(productId, quantity);

        Integer newStock = inventoryService.getAvailableStock(productId);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("quantityAdded", quantity);
        result.put("newAvailableStock", newStock);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Stock added successfully", result));
    }

    @PostMapping("/{productId}/reserve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER', 'SYSTEM')")
    @Operation(summary = "Reserve stock for pending order (internal use)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reserveStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {

        log.info("Reserving {} units for product: {}", quantity, productId);

        inventoryService.reserveStock(productId, quantity);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("quantityReserved", quantity);
        result.put("availableStock", inventoryService.getAvailableStock(productId));
        result.put("reservedStock", inventoryService.getReservedStock(productId));

        return ResponseEntity.ok(ApiResponse.success("Stock reserved successfully", result));
    }

    @PostMapping("/{productId}/release")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER', 'SYSTEM')")
    @Operation(summary = "Release reserved stock (e.g., cancelled order)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> releaseReservedStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {

        log.info("Releasing {} reserved units for product: {}", quantity, productId);

        inventoryService.releaseReservedStock(productId, quantity);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("quantityReleased", quantity);
        result.put("availableStock", inventoryService.getAvailableStock(productId));
        result.put("reservedStock", inventoryService.getReservedStock(productId));

        return ResponseEntity.ok(ApiResponse.success("Reserved stock released successfully", result));
    }

    @PostMapping("/{productId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER', 'SYSTEM')")
    @Operation(summary = "Confirm sale and reduce reserved stock (after payment)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmStockReduction(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {

        log.info("Confirming sale of {} units for product: {}", quantity, productId);

        inventoryService.confirmStockReduction(productId, quantity);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("quantityConfirmed", quantity);
        result.put("availableStock", inventoryService.getAvailableStock(productId));
        result.put("reservedStock", inventoryService.getReservedStock(productId));

        return ResponseEntity.ok(ApiResponse.success("Sale confirmed and stock reduced", result));
    }
}