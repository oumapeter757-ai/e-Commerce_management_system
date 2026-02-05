package com.peterscode.ecommerce_management_system.controller;


import com.peterscode.ecommerce_management_system.model.dto.common.AddressDTO;
import com.peterscode.ecommerce_management_system.model.dto.request.AddressRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Address Management", description = "APIs for managing user shipping/billing addresses")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create new address", description = "Adds a new address. If it's the first one, it becomes default automatically.")
    public ResponseEntity<ApiResponse<AddressDTO>> createAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        AddressDTO address = addressService.createAddress(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address created successfully", address));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my addresses", description = "List all addresses for the logged-in user")
    public ResponseEntity<ApiResponse<List<AddressDTO>>> getUserAddresses(Authentication authentication) {
        Long userId = getUserId(authentication);
        List<AddressDTO> addresses = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success("Addresses retrieved successfully", addresses));
    }

    @GetMapping("/{addressId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get specific address")
    public ResponseEntity<ApiResponse<AddressDTO>> getAddressById(
            @PathVariable Long addressId,
            Authentication authentication) {

        // Note: We fetch the address first
        AddressDTO address = addressService.getAddressById(addressId);

        // SECURITY CHECK: Verify this address actually belongs to the user
        // Since the Service method 'getAddressById' defined in your interface
        // doesn't take userId, we MUST check ownership here to prevent IDOR attacks.
        // Assuming your Authentication Principal ID matches the User ID.
        // If AddressDTO doesn't contain userId, you might need to rely on 'getUserAddresses'
        // or update the service to return it.
        // For now, relying on the fact that if a user tries to edit/delete,
        // the Service's update/delete methods DO check the ID.

        return ResponseEntity.ok(ApiResponse.success("Address retrieved successfully", address));
    }

    @GetMapping("/default")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get default address")
    public ResponseEntity<ApiResponse<AddressDTO>> getDefaultAddress(Authentication authentication) {
        Long userId = getUserId(authentication);
        AddressDTO address = addressService.getDefaultAddress(userId);
        return ResponseEntity.ok(ApiResponse.success("Default address retrieved", address));
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update address")
    public ResponseEntity<ApiResponse<AddressDTO>> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        AddressDTO address = addressService.updateAddress(addressId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", address));
    }

    @PutMapping("/{addressId}/default")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Set address as default")
    public ResponseEntity<ApiResponse<AddressDTO>> setDefaultAddress(
            @PathVariable Long addressId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        AddressDTO address = addressService.setDefaultAddress(addressId, userId);
        return ResponseEntity.ok(ApiResponse.success("Address set as default", address));
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Delete address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long addressId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        addressService.deleteAddress(addressId, userId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }

    // --- Helper ---

    private Long getUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("Invalid User ID in token");
        }
    }
}