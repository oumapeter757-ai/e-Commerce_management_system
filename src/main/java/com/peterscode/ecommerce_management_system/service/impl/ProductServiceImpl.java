package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.ProductMapper;
import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.ProductResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.ProductRequest;
import com.peterscode.ecommerce_management_system.model.entity.Category;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.repository.CategoryRepository;
import com.peterscode.ecommerce_management_system.repository.ProductRepository;
import com.peterscode.ecommerce_management_system.repository.UserRepository;
import com.peterscode.ecommerce_management_system.security.SecurityUtils;
import com.peterscode.ecommerce_management_system.service.InventoryService; // <--- ADDED IMPORT
import com.peterscode.ecommerce_management_system.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final SecurityUtils securityUtils;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        log.debug("Creating new product: {}", request.getName());

        // Check if SKU already exists
        if (productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Product with SKU '" + request.getSku() + "' already exists");
        }

        // Validate discount price
        if (request.getDiscountPrice() != null &&
                request.getDiscountPrice().compareTo(request.getPrice()) >= 0) {
            throw new BadRequestException("Discount price must be less than the regular price");
        }

        // Get category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        // Get current user as seller
        String currentUserEmail = securityUtils.getCurrentUsername()
                .orElseThrow(() -> new BadRequestException("User must be authenticated"));

        User seller = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create product
        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setSeller(seller);

        // Set defaults if not provided
        if (product.getIsActive() == null) {
            product.setIsActive(true);
        }
        if (product.getIsFeatured() == null) {
            product.setIsFeatured(false);
        }
        if (product.getLowStockThreshold() == null) {
            product.setLowStockThreshold(10);
        }

        Product savedProduct = productRepository.save(product);

        // Increment category product count
        categoryRepository.incrementProductCount(category.getId());

        // --- NEW: Initialize Inventory ---
        // We use the separate InventoryService to handle the stock logic
        Integer initialStock = request.getStock() != null ? request.getStock() : 0;
        inventoryService.restock(savedProduct.getId(), initialStock);

        log.info("Product created successfully: {} with ID: {}", savedProduct.getName(), savedProduct.getId());
        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    @Cacheable(value = "products", key = "#productId")
    public ProductResponse getProductById(Long productId) {
        log.debug("Fetching product by ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Increment view count asynchronously
        productRepository.incrementViewCount(productId);

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#sku")
    public ProductResponse getProductBySku(String sku) {
        log.debug("Fetching product by SKU: {}", sku);

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        log.debug("Fetching all products with pagination: {}", pageable);

        Page<Product> productPage = productRepository.findAll(pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getActiveProducts(Pageable pageable) {
        log.debug("Fetching active products");

        Page<Product> productPage = productRepository.findByIsActiveTrue(pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        log.debug("Fetching products by category: {}", categoryId);

        // Verify category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }

        Page<Product> productPage = productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsBySeller(Long sellerId, Pageable pageable) {
        log.debug("Fetching products by seller: {}", sellerId);

        Page<Product> productPage = productRepository.findBySellerId(sellerId, pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getFeaturedProducts(Pageable pageable) {
        log.debug("Fetching featured products");

        Page<Product> productPage = productRepository.findByIsFeaturedTrue(pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getInStockProducts(Pageable pageable) {
        log.debug("Fetching in-stock products");

        Page<Product> productPage = productRepository.findInStockProducts(pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts() {
        log.debug("Fetching low stock products");

        List<Product> products = productRepository.findLowStockProducts();
        return productMapper.toResponseList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getOutOfStockProducts() {
        log.debug("Fetching out of stock products");

        List<Product> products = productRepository.findOutOfStockProducts();
        return productMapper.toResponseList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        log.debug("Searching products with keyword: {}", keyword);

        Page<Product> productPage = productRepository.searchActiveProducts(keyword, pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> filterProducts(
            Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
            String brand, Pageable pageable) {

        log.debug("Filtering products - category: {}, price: {}-{}, brand: {}",
                categoryId, minPrice, maxPrice, brand);

        Page<Product> productPage = productRepository.filterProducts(
                categoryId, minPrice, maxPrice, brand, pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllActiveBrands() {
        log.debug("Fetching all active brands");
        return productRepository.findAllActiveBrands();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getNewArrivals(Pageable pageable) {
        log.debug("Fetching new arrivals");

        Page<Product> productPage = productRepository.findNewArrivals(pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getBestSellers(Pageable pageable) {
        log.debug("Fetching best sellers");

        Page<Product> productPage = productRepository.findBestSellers(pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsOnSale(Pageable pageable) {
        log.debug("Fetching products on sale");

        Page<Product> productPage = productRepository.findProductsOnSale(pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getTopRatedProducts(Pageable pageable) {
        log.debug("Fetching top rated products");

        Page<Product> productPage = productRepository.findTopRatedProducts(pageable);
        List<ProductResponse> products = productMapper.toResponseList(productPage.getContent());

        return PageResponse.of(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        log.debug("Updating product: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Update category if changed
        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getId())) {
            Category oldCategory = product.getCategory();
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            categoryRepository.decrementProductCount(oldCategory.getId());
            categoryRepository.incrementProductCount(newCategory.getId());
            product.setCategory(newCategory);
        }

        productMapper.updateEntityFromRequest(request, product);
        Product updatedProduct = productRepository.save(product);

        log.info("Product updated successfully: {}", productId);
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public void updateStock(Long productId, int quantity, boolean isAddition) {
        log.debug("Updating stock for product: {} - quantity: {}, isAddition: {}",
                productId, quantity, isAddition);

        // --- UPDATED: Use InventoryService to ensure Inventory table is updated ---
        if (isAddition) {
            inventoryService.restock(productId, quantity);
            // Optionally sync the Product table if you still keep a 'stock' column there:
            productRepository.addStock(productId, quantity);
        } else {
            inventoryService.confirmStockReduction(productId, quantity);
            // Optionally sync the Product table:
            int updated = productRepository.reduceStock(productId, quantity);
            if (updated == 0) {
                throw new BadRequestException("Insufficient stock available in product record");
            }
        }

        log.info("Stock updated for product: {}", productId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public void toggleProductStatus(Long productId, boolean isActive) {
        log.debug("Toggling product status: {} to {}", productId, isActive);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        product.setIsActive(isActive);
        productRepository.save(product);

        log.info("Product status toggled: {} - active: {}", productId, isActive);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public void deleteProduct(Long productId) {
        log.debug("Deleting product: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Decrement category product count
        categoryRepository.decrementProductCount(product.getCategory().getId());

        productRepository.delete(product);
        log.info("Product deleted: {}", productId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalProductsCount() {
        return productRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveProductsCount() {
        return productRepository.countByIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long getProductsCountByCategory(Long categoryId) {
        return productRepository.countByCategoryId(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean skuExists(String sku) {
        return productRepository.existsBySku(sku);
    }
}