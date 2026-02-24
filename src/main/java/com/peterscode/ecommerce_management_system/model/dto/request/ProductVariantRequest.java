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
public class ProductVariantRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    @Size(max = 100, message = "Variant name must not exceed 100 characters")
    private String variantName;

    @Size(max = 50, message = "Color must not exceed 50 characters")
    private String color;

    @Size(max = 50, message = "Size must not exceed 50 characters")
    private String size;

    @Size(max = 100, message = "Material must not exceed 100 characters")
    private String material;

    @DecimalMin(value = "-999999.99", message = "Price adjustment is out of range")
    @DecimalMax(value = "999999.99", message = "Price adjustment is out of range")
    private BigDecimal priceAdjustment;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;
}

