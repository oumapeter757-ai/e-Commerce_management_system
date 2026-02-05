package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.CategoryResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.CategoryRequest;
import com.peterscode.ecommerce_management_system.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Create category (Admin only)
     * POST /api/v1/categories
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {

        log.info("Create category request received: {}", request.getName());
        CategoryResponse category = categoryService.createCategory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", category));
    }

    /**
     * Get category by ID (Public)
     * GET /api/v1/categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        log.debug("Get category by ID: {}", id);
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", category));
    }

    /**
     * Get category by slug (Public)
     * GET /api/v1/categories/slug/{slug}
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        log.debug("Get category by slug: {}", slug);
        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", category));
    }

    /**
     * Get all categories with pagination (Public)
     * GET /api/v1/categories?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("Get all categories - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<CategoryResponse> categories = categoryService.getAllCategories(pageable);

        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    /**
     * Get all active categories (Public)
     * GET /api/v1/categories/active
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories() {
        log.debug("Get active categories");
        List<CategoryResponse> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("Active categories retrieved successfully", categories));
    }

    /**
     * Get parent categories (Public)
     * GET /api/v1/categories/parents
     */
    @GetMapping("/parents")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getParentCategories() {
        log.debug("Get parent categories");
        List<CategoryResponse> categories = categoryService.getParentCategories();
        return ResponseEntity.ok(ApiResponse.success("Parent categories retrieved successfully", categories));
    }

    /**
     * Get active parent categories (Public)
     * GET /api/v1/categories/parents/active
     */
    @GetMapping("/parents/active")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveParentCategories() {
        log.debug("Get active parent categories");
        List<CategoryResponse> categories = categoryService.getActiveParentCategories();
        return ResponseEntity.ok(ApiResponse.success("Active parent categories retrieved successfully", categories));
    }

    /**
     * Get subcategories by parent ID (Public)
     * GET /api/v1/categories/{parentId}/subcategories
     */
    @GetMapping("/{parentId}/subcategories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSubcategoriesByParentId(
            @PathVariable Long parentId) {

        log.debug("Get subcategories for parent: {}", parentId);
        List<CategoryResponse> categories = categoryService.getSubcategoriesByParentId(parentId);
        return ResponseEntity.ok(ApiResponse.success("Subcategories retrieved successfully", categories));
    }

    /**
     * Get active subcategories by parent ID (Public)
     * GET /api/v1/categories/{parentId}/subcategories/active
     */
    @GetMapping("/{parentId}/subcategories/active")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveSubcategoriesByParentId(
            @PathVariable Long parentId) {

        log.debug("Get active subcategories for parent: {}", parentId);
        List<CategoryResponse> categories = categoryService.getActiveSubcategoriesByParentId(parentId);
        return ResponseEntity.ok(ApiResponse.success("Active subcategories retrieved successfully", categories));
    }

    /**
     * Get categories ordered by display order (Public)
     * GET /api/v1/categories/ordered
     */
    @GetMapping("/ordered")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoriesOrdered() {
        log.debug("Get categories ordered by display order");
        List<CategoryResponse> categories = categoryService.getCategoriesOrdered();
        return ResponseEntity.ok(ApiResponse.success("Ordered categories retrieved successfully", categories));
    }

    /**
     * Get active categories ordered (Public)
     * GET /api/v1/categories/ordered/active
     */
    @GetMapping("/ordered/active")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategoriesOrdered() {
        log.debug("Get active categories ordered");
        List<CategoryResponse> categories = categoryService.getActiveCategoriesOrdered();
        return ResponseEntity.ok(ApiResponse.success("Active ordered categories retrieved successfully", categories));
    }

    /**
     * Search categories (Public)
     * GET /api/v1/categories/search?keyword=electronics
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> searchCategories(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Search categories with keyword: {}", keyword);

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<CategoryResponse> categories = categoryService.searchCategories(keyword, pageable);

        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", categories));
    }

    /**
     * Get categories with products (Public)
     * GET /api/v1/categories/with-products
     */
    @GetMapping("/with-products")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategoriesWithProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Get categories with products");

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<CategoryResponse> categories = categoryService.getCategoriesWithProducts(pageable);

        return ResponseEntity.ok(ApiResponse.success("Categories with products retrieved successfully", categories));
    }

    /**
     * Update category (Admin only)
     * PUT /api/v1/categories/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {

        log.info("Update category request: {}", id);
        CategoryResponse category = categoryService.updateCategory(id, request);

        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", category));
    }

    /**
     * Toggle category status (Admin only)
     * PATCH /api/v1/categories/{id}/status?isActive=false
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleCategoryStatus(
            @PathVariable Long id,
            @RequestParam boolean isActive) {

        log.info("Toggle category status: {} to {}", id, isActive);
        categoryService.toggleCategoryStatus(id, isActive);

        String message = isActive ? "Category activated successfully" : "Category deactivated successfully";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * Delete category (Admin only)
     * DELETE /api/v1/categories/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        log.info("Delete category: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully"));
    }

    /**
     * Get category statistics (Admin only)
     * GET /api/v1/categories/stats/summary
     */
    @GetMapping("/stats/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryStats>> getCategoryStats() {
        log.debug("Get category statistics");

        CategoryStats stats = CategoryStats.builder()
                .totalCategories(categoryService.getTotalCategoriesCount())
                .activeCategories(categoryService.getActiveCategoriesCount())
                .parentCategories(categoryService.getParentCategoriesCount())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", stats));
    }

    /**
     * Check if slug exists (Admin only - for validation during creation)
     * GET /api/v1/categories/check-slug?slug=electronics
     */
    @GetMapping("/check-slug")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkSlugExists(@RequestParam String slug) {
        log.debug("Check if slug exists: {}", slug);
        boolean exists = categoryService.slugExists(slug);
        return ResponseEntity.ok(ApiResponse.success("Slug check completed", exists));
    }

    /**
     * Check if name exists (Admin only - for validation during creation)
     * GET /api/v1/categories/check-name?name=Electronics
     */
    @GetMapping("/check-name")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkNameExists(@RequestParam String name) {
        log.debug("Check if name exists: {}", name);
        boolean exists = categoryService.nameExists(name);
        return ResponseEntity.ok(ApiResponse.success("Name check completed", exists));
    }

    @lombok.Data
    @lombok.Builder
    private static class CategoryStats {
        private long totalCategories;
        private long activeCategories;
        private long parentCategories;
    }
}