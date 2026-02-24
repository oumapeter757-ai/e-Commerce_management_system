package com.peterscode.ecommerce_management_system.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to toggle selection state of cart items (Kilimall-style checkout selection)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemSelectionRequest {

    @NotEmpty(message = "Item IDs list cannot be empty")
    private List<Long> itemIds;

    @NotNull(message = "Selection state is required")
    private Boolean selected;
}

