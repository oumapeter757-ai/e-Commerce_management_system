package com.peterscode.ecommerce_management_system.model.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShippingRequest {
    private Long orderId;
    private String carrier;
    private String trackingNumber;
    private String shippingMethod;
    private BigDecimal shippingCost;
    private BigDecimal weight;
    private String dimensions;
    private String trackingUrl;
    private String deliveryInstructions;
}

