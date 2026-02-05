package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.ProductResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.ProductRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    /**
     * Create a new product (Admin/Seller only)
     */
    ProductResponse createProduct(ProductRequest request);

    /**
     * Get product by ID (increments view count)
     */
    ProductResponse getProductById(Long productId);

    /**
     * Get product by SKU
     */
    ProductResponse getProductBySku(String sku);

    /**
     * Get all products with pagination
     */
    PageResponse<ProductResponse> getAllProducts(Pageable pageable);

    /**
     * Get all active products
     */
    PageResponse<ProductResponse> getActiveProducts(Pageable pageable);

    /**
     * Get products by category
     */
    PageResponse<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);

    /**
     * Get products by seller
     */
    PageResponse<ProductResponse> getProductsBySeller(Long sellerId, Pageable pageable);

    /**
     * Get featured products
     */
    PageResponse<ProductResponse> getFeaturedProducts(Pageable pageable);

    /**
     * Get in-stock products
     */
    PageResponse<ProductResponse> getInStockProducts(Pageable pageable);

    /**
     * Get low stock products (Admin/Seller only)
     */
    List<ProductResponse> getLowStockProducts();

    /**
     * Get out of stock products (Admin/Seller only)
     */
    List<ProductResponse> getOutOfStockProducts();

    /**
     * Search products by keyword
     */
    PageResponse<ProductResponse> searchProducts(String keyword, Pageable pageable);

    /**
     * Filter products by category, price range, and brand
     */
    PageResponse<ProductResponse> filterProducts(
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String brand,
            Pageable pageable
    );

    /**
     * Get all active brands
     */
    List<String> getAllActiveBrands();

    /**
     * Get new arrivals
     */
    PageResponse<ProductResponse> getNewArrivals(Pageable pageable);

    /**
     * Get best sellers
     */
    PageResponse<ProductResponse> getBestSellers(Pageable pageable);

    /**
     * Get products on sale
     */
    PageResponse<ProductResponse> getProductsOnSale(Pageable pageable);

    /**
     * Get top rated products
     */
    PageResponse<ProductResponse> getTopRatedProducts(Pageable pageable);

    /**
     * Update product (Admin/Seller only)
     */
    ProductResponse updateProduct(Long productId, ProductRequest request);

    /**
     * Update product stock
     */
    void updateStock(Long productId, int quantity, boolean isAddition);

    /**
     * Toggle product active status (Admin only)
     */
    void toggleProductStatus(Long productId, boolean isActive);

    /**
     * Delete product (Admin only)
     */
    void deleteProduct(Long productId);

    /**
     * Get total products count
     */
    long getTotalProductsCount();

    /**
     * Get active products count
     */
    long getActiveProductsCount();

    /**
     * Get products count by category
     */
    long getProductsCountByCategory(Long categoryId);

    /**
     * Check if SKU exists
     */
    boolean skuExists(String sku);
}