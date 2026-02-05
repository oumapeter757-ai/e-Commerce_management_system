package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.response.CategoryResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.CategoryRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {

    /**
     * Create a new category (Admin only)
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Get category by ID
     */
    CategoryResponse getCategoryById(Long categoryId);

    /**
     * Get category by slug
     */
    CategoryResponse getCategoryBySlug(String slug);

    /**
     * Get all categories with pagination
     */
    PageResponse<CategoryResponse> getAllCategories(Pageable pageable);

    /**
     * Get all active categories
     */
    List<CategoryResponse> getActiveCategories();

    /**
     * Get parent categories (no parent)
     */
    List<CategoryResponse> getParentCategories();

    /**
     * Get active parent categories
     */
    List<CategoryResponse> getActiveParentCategories();

    /**
     * Get subcategories by parent ID
     */
    List<CategoryResponse> getSubcategoriesByParentId(Long parentId);

    /**
     * Get active subcategories by parent ID
     */
    List<CategoryResponse> getActiveSubcategoriesByParentId(Long parentId);

    /**
     * Get categories ordered by display order
     */
    List<CategoryResponse> getCategoriesOrdered();

    /**
     * Get active categories ordered by display order
     */
    List<CategoryResponse> getActiveCategoriesOrdered();

    /**
     * Search categories by keyword
     */
    PageResponse<CategoryResponse> searchCategories(String keyword, Pageable pageable);

    /**
     * Get categories with products (non-empty)
     */
    PageResponse<CategoryResponse> getCategoriesWithProducts(Pageable pageable);

    /**
     * Update category (Admin only)
     */
    CategoryResponse updateCategory(Long categoryId, CategoryRequest request);

    /**
     * Toggle category active status (Admin only)
     */
    void toggleCategoryStatus(Long categoryId, boolean isActive);

    /**
     * Delete category (Admin only)
     */
    void deleteCategory(Long categoryId);

    /**
     * Get total categories count
     */
    long getTotalCategoriesCount();

    /**
     * Get active categories count
     */
    long getActiveCategoriesCount();

    /**
     * Get parent categories count
     */
    long getParentCategoriesCount();

    /**
     * Check if slug exists
     */
    boolean slugExists(String slug);

    /**
     * Check if name exists
     */
    boolean nameExists(String name);
}