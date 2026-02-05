package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.CategoryMapper;
import com.peterscode.ecommerce_management_system.model.dto.response.CategoryResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.CategoryRequest;
import com.peterscode.ecommerce_management_system.model.entity.Category;
import com.peterscode.ecommerce_management_system.repository.CategoryRepository;
import com.peterscode.ecommerce_management_system.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        log.debug("Creating new category: {}", request.getName());

        // Check if slug already exists
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Category with slug '" + request.getSlug() + "' already exists");
        }

        // Check if name already exists
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        // Create category
        Category category = categoryMapper.toEntity(request);

        // Set parent if provided
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentId()));
            category.setParent(parent);
        }

        // Set defaults
        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }
        if (category.getDisplayOrder() == null) {
            category.setDisplayOrder(0);
        }

        Category savedCategory = categoryRepository.save(category);

        log.info("Category created successfully: {} with ID: {}", savedCategory.getName(), savedCategory.getId());
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#categoryId")
    public CategoryResponse getCategoryById(Long categoryId) {
        log.debug("Fetching category by ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#slug")
    public CategoryResponse getCategoryBySlug(String slug) {
        log.debug("Fetching category by slug: {}", slug);

        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));

        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAllCategories(Pageable pageable) {
        log.debug("Fetching all categories with pagination: {}", pageable);

        Page<Category> categoryPage = categoryRepository.findAll(pageable);
        List<CategoryResponse> categories = categoryMapper.toResponseList(categoryPage.getContent());

        return PageResponse.of(
                categories,
                categoryPage.getNumber(),
                categoryPage.getSize(),
                categoryPage.getTotalElements(),
                categoryPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'active'")
    public List<CategoryResponse> getActiveCategories() {
        log.debug("Fetching active categories");

        List<Category> categories = categoryRepository.findActiveOrderedByDisplayOrder();
        return categoryMapper.toResponseList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'parent'")
    public List<CategoryResponse> getParentCategories() {
        log.debug("Fetching parent categories");

        List<Category> categories = categoryRepository.findParentCategories();
        return categoryMapper.toResponseList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'active-parent'")
    public List<CategoryResponse> getActiveParentCategories() {
        log.debug("Fetching active parent categories");

        List<Category> categories = categoryRepository.findActiveParentCategories();
        return categoryMapper.toResponseList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'sub-' + #parentId")
    public List<CategoryResponse> getSubcategoriesByParentId(Long parentId) {
        log.debug("Fetching subcategories for parent: {}", parentId);

        // Verify parent exists
        if (!categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Parent category not found with id: " + parentId);
        }

        List<Category> categories = categoryRepository.findSubcategoriesByParentId(parentId);
        return categoryMapper.toResponseList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'active-sub-' + #parentId")
    public List<CategoryResponse> getActiveSubcategoriesByParentId(Long parentId) {
        log.debug("Fetching active subcategories for parent: {}", parentId);

        // Verify parent exists
        if (!categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Parent category not found with id: " + parentId);
        }

        List<Category> categories = categoryRepository.findActiveSubcategoriesByParentId(parentId);
        return categoryMapper.toResponseList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'ordered'")
    public List<CategoryResponse> getCategoriesOrdered() {
        log.debug("Fetching categories ordered by display order");

        List<Category> categories = categoryRepository.findAllOrderedByDisplayOrder();
        return categoryMapper.toResponseList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'active-ordered'")
    public List<CategoryResponse> getActiveCategoriesOrdered() {
        log.debug("Fetching active categories ordered by display order");

        List<Category> categories = categoryRepository.findActiveOrderedByDisplayOrder();
        return categoryMapper.toResponseList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> searchCategories(String keyword, Pageable pageable) {
        log.debug("Searching categories with keyword: {}", keyword);

        Page<Category> categoryPage = categoryRepository.searchCategories(keyword, pageable);
        List<CategoryResponse> categories = categoryMapper.toResponseList(categoryPage.getContent());

        return PageResponse.of(
                categories,
                categoryPage.getNumber(),
                categoryPage.getSize(),
                categoryPage.getTotalElements(),
                categoryPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategoriesWithProducts(Pageable pageable) {
        log.debug("Fetching categories with products");

        Page<Category> categoryPage = categoryRepository.findCategoriesWithProducts(pageable);
        List<CategoryResponse> categories = categoryMapper.toResponseList(categoryPage.getContent());

        return PageResponse.of(
                categories,
                categoryPage.getNumber(),
                categoryPage.getSize(),
                categoryPage.getTotalElements(),
                categoryPage.getTotalPages()
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        log.debug("Updating category: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        // Check if new name conflicts with existing category
        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
            }
        }

        // Update parent if changed
        if (request.getParentId() != null) {
            // Prevent circular reference
            if (request.getParentId().equals(categoryId)) {
                throw new BadRequestException("Category cannot be its own parent");
            }

            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParent(parent);
        }

        categoryMapper.updateEntityFromRequest(request, category);
        Category updatedCategory = categoryRepository.save(category);

        log.info("Category updated successfully: {}", categoryId);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void toggleCategoryStatus(Long categoryId, boolean isActive) {
        log.debug("Toggling category status: {} to {}", categoryId, isActive);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        category.setIsActive(isActive);
        categoryRepository.save(category);

        log.info("Category status toggled: {} - active: {}", categoryId, isActive);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long categoryId) {
        log.debug("Deleting category: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        // Check if category has products
        if (category.getProductCount() != null && category.getProductCount() > 0) {
            throw new BadRequestException("Cannot delete category with existing products. Please reassign or delete products first.");
        }

        // Check if category has subcategories
        if (category.hasSubcategories()) {
            throw new BadRequestException("Cannot delete category with subcategories. Please delete subcategories first.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted: {}", categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalCategoriesCount() {
        return categoryRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveCategoriesCount() {
        return categoryRepository.countByIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long getParentCategoriesCount() {
        return categoryRepository.countByParentIsNull();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean slugExists(String slug) {
        return categoryRepository.existsBySlug(slug);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nameExists(String name) {
        return categoryRepository.existsByName(name);
    }
}