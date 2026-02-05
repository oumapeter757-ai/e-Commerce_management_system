package com.peterscode.ecommerce_management_system.security;

import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    //@Cacheable(value = "users", key = "#username", unless = "#result == null")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", username);
                    return new UsernameNotFoundException("User not found with email: " + username);
                });

        // Check if account is locked
        if (securityUtils.isAccountLocked(username)) {
            log.warn("Attempt to authenticate locked account: {}", username);
            throw new RuntimeException("Account is temporarily locked. Please try again later.");
        }

        // Check if user is enabled
        if (!user.isEnabled()) {
            log.warn("Attempt to authenticate disabled account: {}", username);
            throw new RuntimeException("Account is disabled. Please contact support.");
        }

        log.debug("User loaded successfully: {}", username);
        return user;
    }

    /**
     * Load user by ID
     */
    @Transactional(readOnly = true)
    //@Cacheable(value = "users", key = "#userId", unless = "#result == null")
    public UserDetails loadUserById(Long userId) {
        log.debug("Loading user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return user;
    }
}