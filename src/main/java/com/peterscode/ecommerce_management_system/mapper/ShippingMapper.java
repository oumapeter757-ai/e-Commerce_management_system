package com.peterscode.ecommerce_management_system.mapper;

import com.peterscode.ecommerce_management_system.model.dto.request.ShippingRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ShippingResponse;
import com.peterscode.ecommerce_management_system.model.entity.Shipping;
import org.springframework.stereotype.Component;

@Component
public class ShippingMapper {

    public ShippingResponse toResponse(Shipping shipping) {
        if (shipping == null) return null;

        ShippingResponse response = new ShippingResponse();
        response.setId(shipping.getId());
        response.setOrderId(shipping.getOrder().getId());
        response.setTrackingNumber(shipping.getTrackingNumber());
        response.setCarrier(shipping.getCarrier());
        response.setStatus(shipping.getStatus());
        response.setShippingMethod(shipping.getShippingMethod());
        response.setShippingCost(shipping.getShippingCost());
        response.setEstimatedDeliveryDate(shipping.getEstimatedDeliveryDate());
        response.setActualDeliveryDate(shipping.getActualDeliveryDate());
        response.setShippedAt(shipping.getShippedAt());
        response.setDeliveredAt(shipping.getDeliveredAt());
        response.setTrackingUrl(shipping.getTrackingUrl());
        response.setDeliveryInstructions(shipping.getDeliveryInstructions());
        response.setExceptionReason(shipping.getExceptionReason());

        return response;
    }

    // Note: Entity creation usually happens in service with Order lookup
}