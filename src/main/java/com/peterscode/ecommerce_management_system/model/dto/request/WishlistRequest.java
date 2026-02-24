package com.peterscode.ecommerce_management_system.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for wishlist operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;
}

