package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.ShippingMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.ShippingRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ShippingResponse;
import com.peterscode.ecommerce_management_system.model.entity.Order;
import com.peterscode.ecommerce_management_system.model.entity.Shipping;
import com.peterscode.ecommerce_management_system.model.entity.Notification;
import com.peterscode.ecommerce_management_system.model.enums.NotificationType;
import com.peterscode.ecommerce_management_system.model.enums.ShippingStatus;
import com.peterscode.ecommerce_management_system.repository.OrderRepository;
import com.peterscode.ecommerce_management_system.repository.ShippingRepository;
import com.peterscode.ecommerce_management_system.service.NotificationService;
import com.peterscode.ecommerce_management_system.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private final ShippingRepository shippingRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final ShippingMapper shippingMapper;

    @Override
    @Transactional
    public ShippingResponse createShippingLabel(ShippingRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (shippingRepository.findByOrderId(request.getOrderId()).isPresent()) {
            throw new RuntimeException("Shipping label already exists for this order");
        }

        Shipping shipping = Shipping.builder()
                .order(order)
                .carrier(request.getCarrier())
                .trackingNumber(request.getTrackingNumber())
                .shippingMethod(request.getShippingMethod())
                .shippingCost(request.getShippingCost())
                .shippingAddress(order.getShippingAddress())
                .status(ShippingStatus.PENDING)
                .estimatedDeliveryDate(LocalDateTime.now().plusDays(5)) // Mock logic
                .weight(request.getWeight())
                .dimensions(request.getDimensions())
                .trackingUrl(request.getTrackingUrl())
                .deliveryInstructions(request.getDeliveryInstructions())
                .build();

        Shipping saved = shippingRepository.save(shipping);
        return shippingMapper.toResponse(saved);
    }

    @Override
    public ShippingResponse getShippingByOrder(Long orderId) {
        Shipping shipping = shippingRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping info not found for order: " + orderId));
        return shippingMapper.toResponse(shipping);
    }

    @Override
    public ShippingResponse getShippingById(Long id) {
        return shippingMapper.toResponse(shippingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping not found")));
    }

    @Override
    @Transactional
    public ShippingResponse updateTracking(Long shippingId, String trackingNumber, String trackingUrl) {
        Shipping shipping = shippingRepository.findById(shippingId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping not found"));

        shipping.setTrackingNumber(trackingNumber);
        shipping.setTrackingUrl(trackingUrl);
        shipping.setStatus(ShippingStatus.READY_TO_SHIP);

        return shippingMapper.toResponse(shippingRepository.save(shipping));
    }

    @Override
    @Transactional
    public ShippingResponse updateStatus(Long shippingId, ShippingStatus status, String locationOrReason) {
        Shipping shipping = shippingRepository.findById(shippingId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping not found"));

        // Use business logic methods from Entity
        switch (status) {
            case SHIPPED -> shipping.markAsShipped();
            case OUT_FOR_DELIVERY -> shipping.markAsOutForDelivery();
            case DELIVERED -> shipping.markAsDelivered();
            case EXCEPTION -> shipping.markAsException(locationOrReason);
            default -> shipping.setStatus(status);
        }

        Shipping saved = shippingRepository.save(shipping);

        // Notify User
        notifyUserOfStatusChange(saved);

        return shippingMapper.toResponse(saved);
    }

    private void notifyUserOfStatusChange(Shipping shipping) {
        String message = "Your order #" + shipping.getOrder().getOrderNumber() + " is now " + shipping.getStatus();
        NotificationType type = NotificationType.ORDER_SHIPPED;

        if (shipping.getStatus() == ShippingStatus.DELIVERED) {
            type = NotificationType.ORDER_DELIVERED;
            message = "Your order has been delivered!";
        }

        notificationService.create(Notification.builder()
                .user(shipping.getOrder().getUser())
                .type(type)
                .title("Order Update")
                .message(message)
                .referenceId(shipping.getOrder().getId())
                .referenceType("Order")
                .build());
    }
}