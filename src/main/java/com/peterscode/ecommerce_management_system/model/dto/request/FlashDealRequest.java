package com.peterscode.ecommerce_management_system.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashDealRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Size(max = 255, message = "Deal name must not exceed 255 characters")
    private String dealName;

    @NotNull(message = "Deal price is required")
    @DecimalMin(value = "0.01", message = "Deal price must be greater than 0")
    private BigDecimal dealPrice;

    @NotNull(message = "Start date is required")
    private LocalDateTime startsAt;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endsAt;

    @Min(value = 1, message = "Stock limit must be at least 1")
    private Integer stockLimit;
}

