package com.peterscode.ecommerce_management_system.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String slug;
    private String description;
    private ParentCategoryInfo parent;
    private List<CategoryResponse> subcategories;
    private String imageUrl;
    private String icon;
    private Integer displayOrder;
    private Boolean isActive;
    private Long productCount;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentCategoryInfo implements Serializable {
        private Long id;
        private String name;
        private String slug;
    }
}