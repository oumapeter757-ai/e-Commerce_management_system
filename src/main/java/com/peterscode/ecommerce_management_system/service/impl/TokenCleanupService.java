package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final VerificationTokenRepository verificationTokenRepository;

    /**
     * Runs automatically every day at 3:00 AM server time.
     * Cron expression format: "Seconds Minutes Hours DayMonth Month DayWeek"
     * "0 0 3 * * ?" = At 03:00:00am every day
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired verification tokens...");

        try {
            LocalDateTime now = LocalDateTime.now();

            // This calls the custom JPQL query we added to your repository earlier
            verificationTokenRepository.deleteExpiredTokens(now);

            log.info("Expired token cleanup completed successfully at {}", now);
        } catch (Exception e) {
            log.error("Failed to cleanup expired tokens: {}", e.getMessage(), e);
        }
    }
}