package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.response.RecentlyViewedResponse;

import java.util.List;

public interface RecentlyViewedService {

    void trackProductView(Long userId, Long productId);

    List<RecentlyViewedResponse> getRecentlyViewed(Long userId, int limit);

    void clearHistory(Long userId);
}

