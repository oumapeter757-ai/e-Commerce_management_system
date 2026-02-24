package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.request.FlashDealRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.FlashDealResponse;
import com.peterscode.ecommerce_management_system.service.FlashDealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/flash-deals")
@RequiredArgsConstructor
@Tag(name = "Flash Deal Management", description = "APIs for time-limited flash deals (Kilimall-style)")
public class FlashDealController {

    private final FlashDealService flashDealService;

    // ==================== PUBLIC ENDPOINTS ====================

    @GetMapping
    @Operation(summary = "Get currently active flash deals (Public)")
    public ResponseEntity<ApiResponse<List<FlashDealResponse>>> getActiveDeals() {
        List<FlashDealResponse> deals = flashDealService.getActiveDeals();
        return ResponseEntity.ok(ApiResponse.success("Active flash deals retrieved", deals));
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get active flash deals with pagination (Public)")
    public ResponseEntity<ApiResponse<Page<FlashDealResponse>>> getActiveDealsPaginated(
            @PageableDefault(size = 12) Pageable pageable) {
        Page<FlashDealResponse> deals = flashDealService.getActiveDeals(pageable);
        return ResponseEntity.ok(ApiResponse.success("Flash deals retrieved", deals));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming flash deals (Public)")
    public ResponseEntity<ApiResponse<List<FlashDealResponse>>> getUpcomingDeals() {
        List<FlashDealResponse> deals = flashDealService.getUpcomingDeals();
        return ResponseEntity.ok(ApiResponse.success("Upcoming deals retrieved", deals));
    }

    @GetMapping("/{dealId}")
    @Operation(summary = "Get flash deal by ID (Public)")
    public ResponseEntity<ApiResponse<FlashDealResponse>> getDealById(@PathVariable Long dealId) {
        FlashDealResponse deal = flashDealService.getDealById(dealId);
        return ResponseEntity.ok(ApiResponse.success("Flash deal retrieved", deal));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get active flash deal for a product (Public)")
    public ResponseEntity<ApiResponse<FlashDealResponse>> getDealByProduct(@PathVariable Long productId) {
        FlashDealResponse deal = flashDealService.getDealByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("Flash deal retrieved", deal));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a flash deal (Admin)")
    public ResponseEntity<ApiResponse<FlashDealResponse>> createDeal(
            @Valid @RequestBody FlashDealRequest request) {
        FlashDealResponse deal = flashDealService.createDeal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flash deal created", deal));
    }

    @PutMapping("/{dealId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a flash deal (Admin)")
    public ResponseEntity<ApiResponse<FlashDealResponse>> updateDeal(
            @PathVariable Long dealId,
            @Valid @RequestBody FlashDealRequest request) {
        FlashDealResponse deal = flashDealService.updateDeal(dealId, request);
        return ResponseEntity.ok(ApiResponse.success("Flash deal updated", deal));
    }

    @DeleteMapping("/{dealId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a flash deal (Admin)")
    public ResponseEntity<ApiResponse<Void>> deactivateDeal(@PathVariable Long dealId) {
        flashDealService.deactivateDeal(dealId);
        return ResponseEntity.ok(ApiResponse.success("Flash deal deactivated"));
    }
}

