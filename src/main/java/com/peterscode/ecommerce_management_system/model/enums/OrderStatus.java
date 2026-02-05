package com.peterscode.ecommerce_management_system.model.enums;

public enum OrderStatus {
    PENDING,           // Order created, awaiting payment
    CONFIRMED,         // Payment confirmed, processing
    PROCESSING,        // Order is being prepared
    SHIPPED,           // Order has been shipped
    OUT_FOR_DELIVERY,  // Order is out for delivery
    DELIVERED,         // Order delivered successfully
    CANCELLED,         // Order cancelled by user/admin
    RETURNED,          // Order returned
    REFUNDED,          // Order refunded
    FAILED             // Order failed
}