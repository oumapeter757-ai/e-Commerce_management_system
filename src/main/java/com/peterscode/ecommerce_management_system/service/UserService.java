package com.peterscode.ecommerce_management_system.service;


import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.UserResponse;
import com.peterscode.ecommerce_management_system.model.enums.Role;
import com.peterscode.ecommerce_management_system.model.dto.request.UpdateUserRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.ChangePasswordRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface UserService {

    /**
     * Get user by ID
     */
    UserResponse getUserById(Long userId);

    /**
     * Get user by email
     */
    UserResponse getUserByEmail(String email);

    /**
     * Get current authenticated user
     */
    UserResponse getCurrentUser();

    /**
     * Get all users with pagination
     */
    PageResponse<UserResponse> getAllUsers(Pageable pageable);

    /**
     * Search users by keyword
     */
    PageResponse<UserResponse> searchUsers(String keyword, Pageable pageable);

    /**
     * Get users by role
     */
    PageResponse<UserResponse> getUsersByRole(Role role, Pageable pageable);

    /**
     * Get only enabled users
     */
    PageResponse<UserResponse> getEnabledUsers(Pageable pageable);

    /**
     * Update user profile
     */
    UserResponse updateUser(Long userId, UpdateUserRequest request);

    /**
     * Update current user profile
     */
    UserResponse updateCurrentUser(UpdateUserRequest request);

    /**
     * Change password
     */
    void changePassword(ChangePasswordRequest request);

    /**
     * Enable/Disable user account
     */
    void toggleUserStatus(Long userId, boolean isEnabled);

    /**
     * Delete user (soft delete by disabling)
     */
    void deleteUser(Long userId);

    /**
     * Get total users count
     */
    long getTotalUsersCount();

    /**
     * Get users count by role
     */
    long getUsersCountByRole(Role role);

    /**
     * Get recently registered users
     */
    List<UserResponse> getRecentlyRegisteredUsers(LocalDateTime since);

    /**
     * Check if email exists
     */
    boolean emailExists(String email);
}