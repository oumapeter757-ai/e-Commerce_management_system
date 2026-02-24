package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.InsufficientStockException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.InventoryMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.InventoryUpdateRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.InventoryResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import com.peterscode.ecommerce_management_system.model.entity.Inventory;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import com.peterscode.ecommerce_management_system.repository.InventoryRepository;
import com.peterscode.ecommerce_management_system.repository.ProductRepository;
import com.peterscode.ecommerce_management_system.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product id: " + productId));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional
    public void restock(Long productId, Integer quantity) {
        validateQuantity(quantity);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> createNewInventory(productId));

        inventory.setAvailableStock(inventory.getAvailableStock() + quantity);
        inventory.setLastRestocked(LocalDateTime.now());
        inventoryRepository.save(inventory);

        log.info("Restocked product {}. Added: {}, New Available Stock: {}",
                productId, quantity, inventory.getAvailableStock());
    }

    @Override
    @Transactional
    public InventoryResponse updateStock(Long productId, InventoryUpdateRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));

        // Apply updates with validation
        if (request.getAvailableStock() != null) {
            validateStockValue(request.getAvailableStock());
            inventory.setAvailableStock(request.getAvailableStock());
        }
        if (request.getReservedStock() != null) {
            validateStockValue(request.getReservedStock());
            validateReservedDoesNotExceedAvailable(inventory, request.getReservedStock());
            inventory.setReservedStock(request.getReservedStock());
        }
        if (request.getLowStockThreshold() != null) {
            validateStockValue(request.getLowStockThreshold());
            inventory.setLowStockThreshold(request.getLowStockThreshold());
        }
        if (request.getRestockQuantity() != null) {
            validateStockValue(request.getRestockQuantity());
            inventory.setRestockQuantity(request.getRestockQuantity());
        }

        Inventory savedInventory = inventoryRepository.save(inventory);
        log.info("Updated inventory for product {}. Available: {}, Reserved: {}",
                productId, savedInventory.getAvailableStock(), savedInventory.getReservedStock());

        return inventoryMapper.toResponse(savedInventory);
    }

    @Override
    @Transactional
    public void reserveStock(Long productId, Integer quantity) {
        validateQuantity(quantity);

        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));

        // Business logic: Check if enough stock is available
        if (inventory.getAvailableStock() < quantity) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                            inventory.getProduct().getName(), inventory.getAvailableStock(), quantity));
        }

        // Reserve the quantity (move from available to reserved)
        inventory.setAvailableStock(inventory.getAvailableStock() - quantity);
        inventory.setReservedStock(inventory.getReservedStock() + quantity);

        inventoryRepository.save(inventory);

        log.info("Reserved {} items for product {}. Available: {}, Reserved: {}",
                quantity, productId, inventory.getAvailableStock(), inventory.getReservedStock());
    }

    @Override
    @Transactional
    public void releaseReservedStock(Long productId, Integer quantity) {
        validateQuantity(quantity);

        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));

        // Business logic: Check if enough reserved stock exists
        if (inventory.getReservedStock() < quantity) {
            log.warn("Attempted to release more reserved stock than exists for product {}. Reserved: {}, Requested: {}",
                    productId, inventory.getReservedStock(), quantity);
            // Release whatever is available
            quantity = inventory.getReservedStock();
        }

        // Release reserved stock (move from reserved back to available)
        inventory.setReservedStock(inventory.getReservedStock() - quantity);
        inventory.setAvailableStock(inventory.getAvailableStock() + quantity);

        inventoryRepository.save(inventory);

        log.info("Released {} reserved items for product {}. Available: {}, Reserved: {}",
                quantity, productId, inventory.getAvailableStock(), inventory.getReservedStock());
    }

    @Override
    @Transactional
    public void confirmStockReduction(Long productId, Integer quantity) {
        validateQuantity(quantity);

        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));

        // Business logic: Check if enough reserved stock exists
        if (inventory.getReservedStock() < quantity) {
            throw new IllegalArgumentException(
                    String.format("Cannot confirm more than reserved. Product: %s, Reserved: %d, Requested: %d",
                            inventory.getProduct().getName(), inventory.getReservedStock(), quantity));
        }

        // Confirm sale (remove from reserved, total stock is reduced)
        inventory.setReservedStock(inventory.getReservedStock() - quantity);
        // Available stock was already reduced during reservation

        inventoryRepository.save(inventory);

        log.info("Confirmed sale of {} items for product {}. Reserved stock now: {}",
                quantity, productId, inventory.getReservedStock());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getAvailableStock(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(Inventory::getAvailableStock)
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getLowStockInventory(Pageable pageable) {
        Page<Inventory> page = inventoryRepository.findLowStockInventories(pageable);
        List<InventoryResponse> content = page.getContent().stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStockAvailable(Long productId, Integer quantity) {
        validateQuantity(quantity);

        return inventoryRepository.findByProductId(productId)
                .map(inventory -> inventory.getAvailableStock() >= quantity)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalStock(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(inventory -> inventory.getAvailableStock() + inventory.getReservedStock())
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLowStock(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(inventory -> inventory.getAvailableStock() <= inventory.getLowStockThreshold())
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getReservedStock(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(Inventory::getReservedStock)
                .orElse(0);
    }

    @Override
    public Integer getStock(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(inventory -> inventory.getAvailableStock() + inventory.getReservedStock())
                .orElse(0);
    }


    // ========== HELPER METHODS ==========

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive number");
        }
    }

    private void validateStockValue(Integer stock) {
        if (stock != null && stock < 0) {
            throw new IllegalArgumentException("Stock value cannot be negative");
        }
    }

    private void validateReservedDoesNotExceedAvailable(Inventory inventory, Integer reservedStock) {
        if (reservedStock > inventory.getAvailableStock() + inventory.getReservedStock()) {
            throw new IllegalArgumentException(
                    String.format("Reserved stock cannot exceed total stock. Available: %d, Reserved: %d, Requested: %d",
                            inventory.getAvailableStock(), inventory.getReservedStock(), reservedStock));
        }
    }

    private Inventory createNewInventory(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Inventory inventory = Inventory.builder()
                .product(product)
                .sku(product.getSku() + "-INV")
                .availableStock(0)
                .reservedStock(0)
                .lowStockThreshold(10)
                .restockQuantity(100)
                .build();

        log.info("Created new inventory for product: {}", product.getName());
        return inventoryRepository.save(inventory);
    }
}