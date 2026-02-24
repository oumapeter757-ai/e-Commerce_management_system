package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.WishlistMapper;
import com.peterscode.ecommerce_management_system.model.dto.response.RecentlyViewedResponse;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import com.peterscode.ecommerce_management_system.model.entity.RecentlyViewedProduct;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.repository.ProductRepository;
import com.peterscode.ecommerce_management_system.repository.RecentlyViewedRepository;
import com.peterscode.ecommerce_management_system.repository.UserRepository;
import com.peterscode.ecommerce_management_system.service.RecentlyViewedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecentlyViewedServiceImpl implements RecentlyViewedService {

    private final RecentlyViewedRepository recentlyViewedRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistMapper wishlistMapper;

    /** Max recently viewed products to keep per user */
    private static final int MAX_RECENTLY_VIEWED = 50;

    @Override
    @Transactional
    public void trackProductView(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Update existing record or create new
        Optional<RecentlyViewedProduct> existing =
                recentlyViewedRepository.findByUserIdAndProductId(userId, productId);

        if (existing.isPresent()) {
            existing.get().setViewedAt(LocalDateTime.now());
            recentlyViewedRepository.save(existing.get());
        } else {
            RecentlyViewedProduct rv = RecentlyViewedProduct.builder()
                    .user(user)
                    .product(product)
                    .viewedAt(LocalDateTime.now())
                    .build();
            recentlyViewedRepository.save(rv);
        }

        // Trim if exceeds max per user
        long count = recentlyViewedRepository.countByUserId(userId);
        if (count > MAX_RECENTLY_VIEWED) {
            List<RecentlyViewedProduct> all = recentlyViewedRepository
                    .findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(0, (int) count));

            if (all.size() > MAX_RECENTLY_VIEWED) {
                List<RecentlyViewedProduct> toDelete = all.subList(MAX_RECENTLY_VIEWED, all.size());
                recentlyViewedRepository.deleteAll(toDelete);
            }
        }
    }

    @Override
    public List<RecentlyViewedResponse> getRecentlyViewed(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_RECENTLY_VIEWED);

        return recentlyViewedRepository
                .findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(0, safeLimit))
                .stream()
                .map(wishlistMapper::toRecentlyViewedResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clearHistory(Long userId) {
        recentlyViewedRepository.deleteByUserId(userId);
        log.info("Recently viewed history cleared for user {}", userId);
    }

    /** Cleanup old records (older than 90 days) - runs daily at 4 AM */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void cleanupOldRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        recentlyViewedRepository.deleteOlderThan(cutoff);
        log.info("Cleaned up recently viewed records older than {}", cutoff);
    }
}

