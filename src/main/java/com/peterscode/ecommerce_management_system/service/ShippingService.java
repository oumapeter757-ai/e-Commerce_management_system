package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.request.ShippingRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ShippingResponse;
import com.peterscode.ecommerce_management_system.model.enums.ShippingStatus;

public interface ShippingService {
    ShippingResponse createShippingLabel(ShippingRequest request);
    ShippingResponse getShippingByOrder(Long orderId);
    ShippingResponse updateTracking(Long shippingId, String trackingNumber, String trackingUrl);
    ShippingResponse updateStatus(Long shippingId, ShippingStatus status, String locationOrReason);
    ShippingResponse getShippingById(Long id);
}