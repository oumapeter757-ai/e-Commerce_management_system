package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    Optional<Inventory> findBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);

    // List version
    @Query("SELECT i FROM Inventory i WHERE i.availableStock <= i.lowStockThreshold")
    List<Inventory> findLowStockInventories();

    // Page version (add this)
    @Query("SELECT i FROM Inventory i WHERE i.availableStock <= i.lowStockThreshold")
    Page<Inventory> findLowStockInventories(Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE i.availableStock <= :threshold")
    Page<Inventory> findByAvailableStockLessThanEqual(@Param("threshold") Integer threshold, Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE i.product.id IN :productIds")
    List<Inventory> findByProductIds(@Param("productIds") List<Long> productIds);

    boolean existsByProductId(Long productId);

    @Query("SELECT COALESCE(SUM(i.availableStock), 0) FROM Inventory i WHERE i.product.id = :productId")
    Integer getTotalAvailableStock(@Param("productId") Long productId);
}