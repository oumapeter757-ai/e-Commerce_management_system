package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.model.dto.UserRegistrationRequest;
import com.peterscode.ecommerce_management_system.model.dto.UserResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.UserUpdateRequest;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.repository.UserRepository;
import com.peterscode.ecommerce_management_system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse registerUser(UserRegistrationRequest request) {
        // ... (Registration logic from previous steps) ...
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already taken");
        }
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .password(passwordEncoder.encode(request.getPassword()))
                .isEnabled(true)
                .build();

        return mapToResponse(userRepository.save(user));
    }

    /**
     * UPDATE PROFILE
     * Trigger: User updates their name or phone number.
     * Cache Action: DELETE the old entry from Redis so the next fetch gets the new name.
     */
    @Override
    @Transactional
    @CacheEvict(value = "user_details", key = "#email") // Matches the key in CustomUserDetailsService
    public UserResponse updateUser(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update fields
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());

        // Save to MySQL
        User updatedUser = userRepository.save(user);

        return mapToResponse(updatedUser);
    }

    /**
     * CHANGE PASSWORD
     * Trigger: User changes password.
     * Cache Action: CRITICAL! Delete from Redis immediately.
     * If we don't do this, the old password hash might remain active in cache.
     */
    @Override
    @Transactional
    @CacheEvict(value = "user_details", key = "#email")
    public void changePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // Helper mapper
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}