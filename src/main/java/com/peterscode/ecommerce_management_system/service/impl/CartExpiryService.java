package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.model.entity.Cart;
import com.peterscode.ecommerce_management_system.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service to clean up expired guest carts.
 * Prevents database bloat from abandoned guest sessions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartExpiryService {

    private final CartRepository cartRepository;

    /**
     * Delete expired guest carts every hour.
     * Only affects carts with no user (guest carts) that have passed their expiry date.
     */
    @Scheduled(fixedDelay = 3600000) // Every hour
    @Transactional
    public void cleanupExpiredGuestCarts() {
        LocalDateTime now = LocalDateTime.now();

        // Also clean up old abandoned guest carts (no expiry set, older than 30 days)
        LocalDateTime abandonedCutoff = now.minusDays(30);
        cartRepository.deleteAbandonedGuestCarts(abandonedCutoff);

        log.debug("Expired guest cart cleanup completed at {}", now);
    }
}

