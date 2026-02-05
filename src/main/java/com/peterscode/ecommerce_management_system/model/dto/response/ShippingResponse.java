// Response DTO
package com.peterscode.ecommerce_management_system.model.dto.response;

import com.peterscode.ecommerce_management_system.model.enums.ShippingStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShippingResponse {
    private Long id;
    private Long orderId;
    private String trackingNumber;
    private String carrier;
    private ShippingStatus status;
    private String shippingMethod;
    private BigDecimal shippingCost;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private String trackingUrl;
    private String deliveryInstructions;
    private String exceptionReason;
}