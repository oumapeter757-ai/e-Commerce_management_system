package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.request.LoginRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.RegisterRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.LoginResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

public interface AuthService {

    /**
     * Register a new customer (public registration)
     * Only @gmail.com emails allowed
     * Email verification required
     */
    @Transactional
    UserResponse registerCustomer(RegisterRequest request, HttpServletRequest httpRequest);

    /**
     * Register admin/seller/support user (admin only)
     * Only @gmail.com emails allowed
     * Email verification required
     */
    @Transactional
    UserResponse registerUserByAdmin(RegisterRequest request, String role, HttpServletRequest httpRequest);

    /**
     * Authenticate user and generate tokens (email must be verified)
     */
    @Transactional
    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);

    /**
     * Logout user and invalidate tokens
     */
    @Transactional
    void logout(HttpServletRequest httpRequest);

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    LoginResponse refreshToken(String refreshToken, HttpServletRequest httpRequest);

    /**
     * Verify email with token (24-hour expiry)
     */
    @Transactional
    void verifyEmail(String token, HttpServletRequest httpRequest);

    /**
     * Resend verification email
     */
    @Transactional
    void resendVerificationEmail(String email, HttpServletRequest httpRequest);

    /**
     * Request password reset (sends email with 1-hour reset link)
     */
    @Transactional
    void requestPasswordReset(String email, HttpServletRequest httpRequest);

    /**
     * Reset password with token
     */
    @Transactional
    void resetPassword(String token, String newPassword, HttpServletRequest httpRequest);
}