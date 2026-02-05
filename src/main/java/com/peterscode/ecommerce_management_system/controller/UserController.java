package com.peterscode.ecommerce_management_system.controller;


import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.PageResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.UserResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.ChangePasswordRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.UpdateUserRequest;
import com.peterscode.ecommerce_management_system.model.enums.Role;
import com.peterscode.ecommerce_management_system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        log.debug("Fetching current user profile");
        UserResponse user = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", user));
    }

    /**
     * Update current user profile
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserProfile(
            @Valid @RequestBody UpdateUserRequest request) {
        log.debug("Updating current user profile");
        UserResponse user = userService.updateCurrentUser(request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", user));
    }

    /**
     * Change password
     */
    @PostMapping("/profile/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        log.debug("Changing password for current user");
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    /**
     * Get user by ID (Admin/Support only - Customers cannot access)
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    /**
     * Get all users with pagination (Admin/Support only - Customers cannot access)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.debug("Fetching all users - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<UserResponse> users = userService.getAllUsers(pageable);

        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    /**
     * Search users by keyword (Admin/Support only - Customers cannot access)
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("Searching users with keyword: {}", keyword);

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<UserResponse> users = userService.searchUsers(keyword, pageable);

        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", users));
    }

    /**
     * Get users by role (Admin only - Customers and Support cannot access)
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsersByRole(
            @PathVariable Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("Fetching users by role: {}", role);

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<UserResponse> users = userService.getUsersByRole(role, pageable);

        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    /**
     * Update user by ID (Admin only - Customers cannot modify other users)
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {

        log.debug("Updating user: {}", userId);
        UserResponse user = userService.updateUser(userId, request);

        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }

    /**
     * Enable/Disable user (Admin only - Customers cannot modify user status)
     */
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
            @PathVariable Long userId,
            @RequestParam boolean isEnabled) {

        log.debug("Toggling user status: {} to {}", userId, isEnabled);
        userService.toggleUserStatus(userId, isEnabled);

        String message = isEnabled ? "User enabled successfully" : "User disabled successfully";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * Delete user (Admin only - Customers cannot delete users)
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        log.debug("Deleting user: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }

    /**
     * Get total users count (Admin only - Customers cannot access statistics)
     */
    @GetMapping("/stats/total")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getTotalUsersCount() {
        log.debug("Fetching total users count");
        long count = userService.getTotalUsersCount();
        return ResponseEntity.ok(ApiResponse.success("Total users count retrieved", count));
    }

    /**
     * Get users count by role (Admin only - Customers cannot access statistics)
     */
    @GetMapping("/stats/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getUsersCountByRole(@PathVariable Role role) {
        log.debug("Fetching users count for role: {}", role);
        long count = userService.getUsersCountByRole(role);
        return ResponseEntity.ok(ApiResponse.success("Users count retrieved", count));
    }
}