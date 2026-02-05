package com.peterscode.ecommerce_management_system.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 255, message = "Product name must be between 3 and 255 characters")
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(min = 3, max = 100, message = "SKU must be between 3 and 100 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU must contain only uppercase letters, numbers, and hyphens")
    private String sku;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price must be a valid amount")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "Discount price must be greater than or equal to 0")
    @Digits(integer = 10, fraction = 2, message = "Discount price must be a valid amount")
    private BigDecimal discountPrice;

    @DecimalMin(value = "0.00", message = "Cost price must be greater than or equal to 0")
    @Digits(integer = 10, fraction = 2, message = "Cost price must be a valid amount")
    private BigDecimal costPrice;

    @NotNull(message = "Category ID is required")
    @Min(value = 1, message = "Category ID must be a positive number")
    private Long categoryId;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Initial stock is required") // <--- ADD THIS
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;

    @Pattern(regexp = "^(https?://)?([\\w\\-]+\\.)+[\\w\\-]+(/[\\w\\-./?%&=]*)?$",
            message = "Invalid image URL format")
    private String imageUrl;

    private String additionalImages;

    @NotBlank(message = "Brand is required")
    @Size(max = 50, message = "Brand must not exceed 50 characters")
    private String brand;

    @Size(max = 100, message = "Manufacturer must not exceed 100 characters")
    private String manufacturer;

    @DecimalMin(value = "0.00", message = "Weight must be greater than or equal to 0")
    private BigDecimal weight;

    @Size(max = 100, message = "Dimensions must not exceed 100 characters")
    private String dimensions;

    @Size(max = 50, message = "Color must not exceed 50 characters")
    private String color;

    @Size(max = 50, message = "Size must not exceed 50 characters")
    private String size;

    private String tags;

    private Boolean isActive;

    private Boolean isFeatured;
}