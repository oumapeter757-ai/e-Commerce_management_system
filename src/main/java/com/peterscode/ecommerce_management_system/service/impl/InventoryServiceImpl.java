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
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> createNewInventory(productId));

        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);
        log.info("Restocked product {}. New Total Quantity: {}", productId, inventory.getQuantity());
    }

    @Override
    @Transactional
    public InventoryResponse updateStock(Long productId, InventoryUpdateRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        inventory.setQuantity(request.getQuantity()); // Sets absolute quantity
        return inventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    @Override
    @Transactional
    public void reserveStock(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));

        if (inventory.getAvailableStock() < quantity) {
            throw new InsufficientStockException("Not enough stock for product " + productId + ". Requested: " + quantity + ", Available: " + inventory.getAvailableStock());
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);
        log.debug("Reserved {} items for product {}", quantity, productId);
    }

    @Override
    @Transactional
    public void releaseReservedStock(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        int newReserved = inventory.getReservedQuantity() - quantity;
        if (newReserved < 0) newReserved = 0; // Safety net

        inventory.setReservedQuantity(newReserved);
        inventoryRepository.save(inventory);
        log.debug("Released {} reserved items for product {}", quantity, productId);
    }

    @Override
    @Transactional
    public void confirmStockReduction(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        // When a sale is confirmed, we reduce the total quantity AND the reserved quantity
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);

        // Safety check to ensure we don't go negative
        if (inventory.getQuantity() < 0) inventory.setQuantity(0);
        if (inventory.getReservedQuantity() < 0) inventory.setReservedQuantity(0);

        inventoryRepository.save(inventory);
        log.info("Stock reduced by {} for product {}", quantity, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getStock(Long productId) {
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

        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private Inventory createNewInventory(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        return Inventory.builder()
                .product(product)
                .quantity(0)
                .reservedQuantity(0)
                .lowStockThreshold(10)
                .build();
    }
}