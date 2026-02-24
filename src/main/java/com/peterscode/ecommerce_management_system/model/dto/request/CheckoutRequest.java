package com.peterscode.ecommerce_management_system.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kilimall-style checkout: creates order from selected cart items.
 * No need to pass item list — selected cart items are used automatically.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotNull(message = "Shipping address ID is required")
    private Long shippingAddressId;

    @NotNull(message = "Billing address ID is required")
    private Long billingAddressId;

    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    private String couponCode;

    @Size(max = 1000, message = "Customer notes must not exceed 1000 characters")
    private String customerNotes;
}

