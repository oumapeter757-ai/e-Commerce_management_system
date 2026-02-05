package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.request.ShippingRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ShippingResponse;
import com.peterscode.ecommerce_management_system.model.enums.ShippingStatus;
import com.peterscode.ecommerce_management_system.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShippingResponse> createLabel(@RequestBody ShippingRequest request) {
        return ResponseEntity.ok(shippingService.createShippingLabel(request));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ShippingResponse> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(shippingService.getShippingByOrder(orderId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShippingResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ShippingStatus status,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(shippingService.updateStatus(id, status, reason));
    }

    @PatchMapping("/{id}/tracking")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShippingResponse> updateTracking(
            @PathVariable Long id,
            @RequestParam String trackingNumber,
            @RequestParam String trackingUrl) {
        return ResponseEntity.ok(shippingService.updateTracking(id, trackingNumber, trackingUrl));
    }
}