package com.peterscode.ecommerce_management_system.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request for bulk cart operations (remove selected, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCartOperationRequest {

    @NotEmpty(message = "Item IDs list cannot be empty")
    private List<Long> itemIds;
}

