package com.peterscode.ecommerce_management_system.model.dto.request;

import com.peterscode.ecommerce_management_system.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "Order status is required")
    private OrderStatus status;
}