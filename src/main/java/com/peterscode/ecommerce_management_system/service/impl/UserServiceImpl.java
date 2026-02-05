package com.peterscode.ecommerce_management_system.service.impl;


import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.exception.UnauthorizedException;
import com.peterscode.ecommerce_management_system.mapper.UserMapper;
import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.UserResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.ChangePasswordRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.UpdateUserRequest;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.model.enums.Role;
import com.peterscode.ecommerce_management_system.repository.UserRepository;
import com.peterscode.ecommerce_management_system.security.SecurityUtils;
import com.peterscode.ecommerce_management_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#userId")
    public UserResponse getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#email")
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        String email = securityUtils.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("No authenticated user found"));

        log.debug("Fetching current user: {}", email);
        return getUserByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pagination: {}", pageable);
        Page<User> userPage = userRepository.findAll(pageable);
        List<UserResponse> users = userMapper.toResponseList(userPage.getContent());

        return PageResponse.of(
                users,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String keyword, Pageable pageable) {
        log.debug("Searching users with keyword: {}", keyword);
        Page<User> userPage = userRepository.searchUsers(keyword, pageable);
        List<UserResponse> users = userMapper.toResponseList(userPage.getContent());

        return PageResponse.of(
                users,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsersByRole(Role role, Pageable pageable) {
        log.debug("Fetching users by role: {}", role);
        Page<User> userPage = userRepository.findByRole(role, pageable);
        List<UserResponse> users = userMapper.toResponseList(userPage.getContent());

        return PageResponse.of(
                users,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getEnabledUsers(Pageable pageable) {
        log.debug("Fetching enabled users");
        Page<User> userPage = userRepository.findByEnabledTrue(pageable);
        List<UserResponse> users = userMapper.toResponseList(userPage.getContent());

        return PageResponse.of(
                users,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        log.debug("Updating user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        userMapper.updateEntityFromRequest(request, user);
        User updatedUser = userRepository.save(user);

        log.info("User updated successfully: {}", userId);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(UpdateUserRequest request) {
        String email = securityUtils.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("No authenticated user found"));

        log.debug("Updating current user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userMapper.updateEntityFromRequest(request, user);
        User updatedUser = userRepository.save(user);

        log.info("Current user updated successfully: {}", email);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        String email = securityUtils.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("No authenticated user found"));

        log.debug("Changing password for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Verify new password matches confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        // Validate new password strength
        if (!securityUtils.isPasswordStrong(request.getNewPassword())) {
            throw new BadRequestException("New password does not meet strength requirements");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", email);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void toggleUserStatus(Long userId, boolean isEnabled) {
        log.debug("Toggling user status for ID: {} to {}", userId, isEnabled);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setEnabled(isEnabled);
        userRepository.save(user);

        log.info("User status toggled successfully: {} - enabled: {}", userId, isEnabled);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(Long userId) {
        log.debug("Deleting user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Soft delete by disabling the account
        user.setEnabled(false);
        userRepository.save(user);

        log.info("User soft deleted successfully: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalUsersCount() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUsersCountByRole(Role role) {
        return userRepository.countByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getRecentlyRegisteredUsers(LocalDateTime since) {
        log.debug("Fetching users registered since: {}", since);
        List<User> users = userRepository.findRecentlyRegistered(since);
        return userMapper.toResponseList(users);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}