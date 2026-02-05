package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.response.ProductResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.ProductRequest;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import com.peterscode.ecommerce_management_system.model.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {CategoryMapper.class}
)
public interface ProductMapper {

    @Mapping(target = "actualPrice", expression = "java(product.getActualPrice())")
    @Mapping(target = "discountPercentage", expression = "java(product.getDiscountPercentage())")
    @Mapping(target = "inStock", expression = "java(product.isInStock())")
    @Mapping(target = "lowStock", expression = "java(product.isLowStock())")
    @Mapping(target = "seller", source = "seller")
    ProductResponse toResponse(Product product);

    @Mapping(target = "id", source = "seller.id")
    @Mapping(target = "firstName", source = "seller.firstName")
    @Mapping(target = "lastName", source = "seller.lastName")
    @Mapping(target = "email", source = "seller.email")
    ProductResponse.SellerInfo toSellerInfo(User seller);

    List<ProductResponse> toResponseList(List<Product> products);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "soldCount", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "soldCount", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ProductRequest request, @MappingTarget Product product);
}