package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.ProductResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.ProductRequest;
import com.peterscode.ecommerce_management_system.service.ProductService;
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

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Create product (Admin/Seller only)
     * POST /api/v1/products
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        log.info("Create product request received: {}", request.getName());
        ProductResponse product = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    /**
     * Get product by ID (Public)
     * GET /api/v1/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        log.debug("Get product by ID: {}", id);
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    /**
     * Get product by SKU (Public)
     * GET /api/v1/products/sku/{sku}
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(@PathVariable String sku) {
        log.debug("Get product by SKU: {}", sku);
        ProductResponse product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    /**
     * Get all products (Public - shows only active)
     * GET /api/v1/products?page=0&size=10&sortBy=name&sortDir=ASC
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.debug("Get all products - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ProductResponse> products = productService.getActiveProducts(pageable);

        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }

    /**
     * Get all products including inactive (Admin/Seller only)
     * GET /api/v1/products/all?page=0&size=10
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProductsIncludingInactive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.debug("Get all products including inactive");

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ProductResponse> products = productService.getAllProducts(pageable);

        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }

    /**
     * Get products by category (Public)
     * GET /api/v1/products/category/{categoryId}
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("Get products by category: {}", categoryId);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ProductResponse> products = productService.getProductsByCategory(categoryId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }

    /**
     * Get products by seller (Seller/Admin only)
     * GET /api/v1/products/seller/{sellerId}
     */
    @GetMapping("/seller/{sellerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsBySeller(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.debug("Get products by seller: {}", sellerId);

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ProductResponse> products = productService.getProductsBySeller(sellerId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }

    /**
     * Get featured products (Public)
     * GET /api/v1/products/featured
     */
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getFeaturedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.debug("Get featured products");

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ProductResponse> products = productService.getFeaturedProducts(pageable);

        return ResponseEntity.ok(ApiResponse.success("Featured products retrieved successfully", products));
    }

    /**
     * Get new arrivals (Public)
     * GET /api/v1/products/new-arrivals
     */
    @GetMapping("/new-arrivals")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getNewArrivals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.debug("Get new arrivals");

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ProductResponse> products = productService.getNewArrivals(pageable);

        return ResponseEntity.ok(ApiResponse.success("New arrivals retrieved successfully", products));
    }

    /**
     * Get best sellers (Public)
     * GET /api/v1/products/best-sellers
     */
    @GetMapping("/best-sellers")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getBestSellers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.debug("Get best sellers");

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ProductResponse> products = productService.getBestSellers(pageable);

        return ResponseEntity.ok(ApiResponse.success("Best sellers retrieved successfully", products));
    }

    /**
     * Get products on sale (Public)
     * GET /api/v1/products/on-sale
     */
    @GetMapping("/on-sale")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsOnSale(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.debug("Get products on sale");

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ProductResponse> products = productService.getProductsOnSale(pageable);

        return ResponseEntity.ok(ApiResponse.success("Products on sale retrieved successfully", products));
    }

    /**
     * Get top rated products (Public)
     * GET /api/v1/products/top-rated
     */
    @GetMapping("/top-rated")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getTopRatedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.debug("Get top rated products");

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ProductResponse> products = productService.getTopRatedProducts(pageable);

        return ResponseEntity.ok(ApiResponse.success("Top rated products retrieved successfully", products));
    }

    /**
     * Search products (Public)
     * GET /api/v1/products/search?keyword=laptop
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.debug("Search products with keyword: {}", keyword);

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ProductResponse> products = productService.searchProducts(keyword, pageable);

        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", products));
    }

    /**
     * Filter products (Public)
     * GET /api/v1/products/filter?categoryId=1&minPrice=100&maxPrice=1000&brand=Apple
     */
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> filterProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("Filter products - category: {}, price: {}-{}, brand: {}",
                categoryId, minPrice, maxPrice, brand);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ProductResponse> products = productService.filterProducts(
                categoryId, minPrice, maxPrice, brand, pageable);

        return ResponseEntity.ok(ApiResponse.success("Filtered products retrieved successfully", products));
    }

    /**
     * Get all brands (Public)
     * GET /api/v1/products/brands
     */
    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<String>>> getAllBrands() {
        log.debug("Get all brands");
        List<String> brands = productService.getAllActiveBrands();
        return ResponseEntity.ok(ApiResponse.success("Brands retrieved successfully", brands));
    }

    /**
     * Get low stock products (Admin/Seller only)
     * GET /api/v1/products/low-stock
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts() {
        log.debug("Get low stock products");
        List<ProductResponse> products = productService.getLowStockProducts();
        return ResponseEntity.ok(ApiResponse.success("Low stock products retrieved successfully", products));
    }

    /**
     * Get out of stock products (Admin/Seller only)
     * GET /api/v1/products/out-of-stock
     */
    @GetMapping("/out-of-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getOutOfStockProducts() {
        log.debug("Get out of stock products");
        List<ProductResponse> products = productService.getOutOfStockProducts();
        return ResponseEntity.ok(ApiResponse.success("Out of stock products retrieved successfully", products));
    }

    /**
     * Update product (Admin/Seller only)
     * PUT /api/v1/products/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        log.info("Update product request: {}", id);
        ProductResponse product = productService.updateProduct(id, request);

        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
    }

    /**
     * Update product stock (Admin/Seller only)
     * PATCH /api/v1/products/{id}/stock?quantity=10&isAddition=true
     */
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<Void>> updateStock(
            @PathVariable Long id,
            @RequestParam int quantity,
            @RequestParam(defaultValue = "true") boolean isAddition) {

        log.info("Update stock for product: {} - quantity: {}, isAddition: {}", id, quantity, isAddition);
        productService.updateStock(id, quantity, isAddition);

        String message = isAddition ? "Stock added successfully" : "Stock reduced successfully";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * Toggle product status (Admin only)
     * PATCH /api/v1/products/{id}/status?isActive=false
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleProductStatus(
            @PathVariable Long id,
            @RequestParam boolean isActive) {

        log.info("Toggle product status: {} to {}", id, isActive);
        productService.toggleProductStatus(id, isActive);

        String message = isActive ? "Product activated successfully" : "Product deactivated successfully";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * Delete product (Admin only)
     * DELETE /api/v1/products/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("Delete product: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    /**
     * Get products statistics (Admin only)
     * GET /api/v1/products/stats/summary
     */
    @GetMapping("/stats/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductStats>> getProductStats() {
        log.debug("Get product statistics");

        ProductStats stats = ProductStats.builder()
                .totalProducts(productService.getTotalProductsCount())
                .activeProducts(productService.getActiveProductsCount())
                .lowStockCount(productService.getLowStockProducts().size())
                .outOfStockCount(productService.getOutOfStockProducts().size())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", stats));
    }

    @lombok.Data
    @lombok.Builder
    private static class ProductStats {
        private long totalProducts;
        private long activeProducts;
        private int lowStockCount;
        private int outOfStockCount;
    }
}