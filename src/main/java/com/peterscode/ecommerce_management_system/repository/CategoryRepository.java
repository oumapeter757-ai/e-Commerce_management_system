package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    Optional<Category> findByName(String name);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    Page<Category> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL")
    List<Category> findParentCategories();

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.isActive = true")
    List<Category> findActiveParentCategories();

    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId")
    List<Category> findSubcategoriesByParentId(@Param("parentId") Long parentId);

    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.isActive = true")
    List<Category> findActiveSubcategoriesByParentId(@Param("parentId") Long parentId);

    @Query("SELECT c FROM Category c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Category> searchCategories(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Category c ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findAllOrderedByDisplayOrder();

    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findActiveOrderedByDisplayOrder();

    @Modifying
    @Query("UPDATE Category c SET c.productCount = c.productCount + 1 WHERE c.id = :categoryId")
    void incrementProductCount(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("UPDATE Category c SET c.productCount = c.productCount - 1 WHERE c.id = :categoryId AND c.productCount > 0")
    void decrementProductCount(@Param("categoryId") Long categoryId);

    @Query("SELECT c FROM Category c WHERE c.productCount > 0 ORDER BY c.productCount DESC")
    Page<Category> findCategoriesWithProducts(Pageable pageable);

    long countByIsActiveTrue();

    long countByParentIsNull();
}