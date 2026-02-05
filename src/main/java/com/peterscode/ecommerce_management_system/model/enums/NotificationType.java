package com.peterscode.ecommerce_management_system.model.enums;

public enum NotificationType {
    // Order notifications
    ORDER_PLACED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    ORDER_REFUNDED,

    // Payment notifications
    PAYMENT_RECEIVED,
    PAYMENT_FAILED,
    REFUND_PROCESSED,

    // Product notifications
    PRODUCT_BACK_IN_STOCK,
    PRODUCT_PRICE_DROP,
    PRODUCT_LOW_STOCK,

    // Review notifications
    REVIEW_APPROVED,
    REVIEW_REJECTED,
    REVIEW_REPLY,

    // User notifications
    ACCOUNT_CREATED,
    PASSWORD_CHANGED,
    EMAIL_VERIFIED,

    // Promotional notifications
    PROMOTIONAL_OFFER,
    DISCOUNT_AVAILABLE,
    NEW_ARRIVAL,

    // System notifications
    SYSTEM_MAINTENANCE,
    SYSTEM_UPDATE,
    SECURITY_ALERT,

    // General
    GENERAL_INFO,
    PAYMENT_REFUNDED, REMINDER
}