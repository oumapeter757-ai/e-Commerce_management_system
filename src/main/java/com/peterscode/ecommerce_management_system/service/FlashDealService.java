package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.request.FlashDealRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.FlashDealResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FlashDealService {

    FlashDealResponse createDeal(FlashDealRequest request);

    FlashDealResponse getDealById(Long dealId);

    List<FlashDealResponse> getActiveDeals();

    Page<FlashDealResponse> getActiveDeals(Pageable pageable);

    List<FlashDealResponse> getUpcomingDeals();

    FlashDealResponse getDealByProductId(Long productId);

    FlashDealResponse updateDeal(Long dealId, FlashDealRequest request);

    void deactivateDeal(Long dealId);

    void incrementSoldCount(Long dealId);
}

