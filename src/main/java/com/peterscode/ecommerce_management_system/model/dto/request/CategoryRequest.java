package com.peterscode.ecommerce_management_system.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(min = 2, max = 150, message = "Slug must be between 2 and 150 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Min(value = 1, message = "Parent category ID must be a positive number")
    private Long parentId;

    @Pattern(regexp = "^(https?://)?([\\w\\-]+\\.)+[\\w\\-]+(/[\\w\\-./?%&=]*)?$",
            message = "Invalid image URL format")
    private String imageUrl;

    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;

    private Boolean isActive;

    @Size(max = 500, message = "Meta title must not exceed 500 characters")
    private String metaTitle;

    @Size(max = 1000, message = "Meta description must not exceed 1000 characters")
    private String metaDescription;

    @Size(max = 500, message = "Meta keywords must not exceed 500 characters")
    private String metaKeywords;
}