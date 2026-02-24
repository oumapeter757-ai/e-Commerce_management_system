package com.peterscode.ecommerce_management_system.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to update cart item notes (color, size, variant specifications)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemNoteRequest {

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}

