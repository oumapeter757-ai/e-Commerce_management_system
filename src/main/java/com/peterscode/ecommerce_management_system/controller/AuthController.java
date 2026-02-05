package com.peterscode.ecommerce_management_system.controller;

import com.peterscode.ecommerce_management_system.model.dto.response.ApiResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.LoginResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.UserResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.LoginRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.RegisterRequest;
import com.peterscode.ecommerce_management_system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new customer (PUBLIC - Anyone can register)
     * POST /api/v1/auth/register/customer
     */
    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<UserResponse>> registerCustomer(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        log.info("Customer registration request received for email: {}", request.getEmail());
        UserResponse user = authService.registerCustomer(request, httpRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful! Please check your email to verify your account.",
                        user
                ));
    }

    /**
     * Register admin/seller/support user (ADMIN ONLY - Must be logged in as admin)
     * POST /api/v1/auth/admin/register?role=ADMIN
     */
    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> registerUserByAdmin(
            @Valid @RequestBody RegisterRequest request,
            @RequestParam String role,
            HttpServletRequest httpRequest) {

        log.info("Admin registration request received for email: {} with role: {}",
                request.getEmail(), role);
        UserResponse user = authService.registerUserByAdmin(request, role, httpRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "User registered successfully! Verification email has been sent.",
                        user
                ));
    }

    /**
     * Login user (SHARED - All users use same login endpoint)
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        log.info("Login request received for email: {}", request.getEmail());
        LoginResponse loginResponse = authService.login(request, httpRequest);

        return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
    }

    /**
     * Logout user
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        log.info("Logout request received");
        authService.logout(httpRequest);

        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    /**
     * Refresh access token
     * POST /api/v1/auth/refresh-token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestBody String refreshToken,
            HttpServletRequest httpRequest) {

        log.info("Token refresh request received");
        LoginResponse loginResponse = authService.refreshToken(refreshToken, httpRequest);

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", loginResponse));
    }

    /**
     * Verify email with token
     * GET /api/v1/auth/verify-email?token=xxx
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token,
            HttpServletRequest httpRequest) {

        log.info("Email verification request received");
        authService.verifyEmail(token, httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "Email verified successfully! You can now login to your account."
        ));
    }

    /**
     * Resend verification email
     * POST /api/v1/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @RequestParam String email,
            HttpServletRequest httpRequest) {

        log.info("Resend verification email request for: {}", email);
        authService.resendVerificationEmail(email, httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "Verification email has been resent. Please check your inbox."
        ));
    }

    /**
     * Request password reset
     * POST /api/v1/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestParam String email,
            HttpServletRequest httpRequest) {

        log.info("Password reset request received for email: {}", email);
        authService.requestPasswordReset(email, httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists with this email, you will receive a password reset link."
        ));
    }

    /**
     * Reset password with token
     * POST /api/v1/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            HttpServletRequest httpRequest) {

        log.info("Password reset confirmation received");
        authService.resetPassword(token, newPassword, httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "Password reset successful! You can now login with your new password."
        ));
    }
}