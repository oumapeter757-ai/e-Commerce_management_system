package com.peterscode.ecommerce_management_system.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payment Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least 1")
    @DecimalMax(value = "999999.0", message = "Amount cannot exceed 999999")
    private BigDecimal amount;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+254|254|0)?[17]\\d{8}$",
            message = "Invalid Kenyan phone number")
    private String phoneNumber;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
