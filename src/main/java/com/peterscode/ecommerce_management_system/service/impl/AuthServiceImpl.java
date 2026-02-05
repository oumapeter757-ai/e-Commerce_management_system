package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.constant.SecurityConstants;
import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.exception.UnauthorizedException;
import com.peterscode.ecommerce_management_system.mapper.UserMapper;
import com.peterscode.ecommerce_management_system.model.dto.response.LoginResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.UserResponse;
import com.peterscode.ecommerce_management_system.model.dto.request.LoginRequest;
import com.peterscode.ecommerce_management_system.model.dto.request.RegisterRequest;
import com.peterscode.ecommerce_management_system.model.entity.TokenType;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.model.entity.VerificationToken;
import com.peterscode.ecommerce_management_system.model.enums.Role;

import com.peterscode.ecommerce_management_system.repository.UserRepository;
import com.peterscode.ecommerce_management_system.repository.VerificationTokenRepository;
import com.peterscode.ecommerce_management_system.security.JwtTokenProvider;
import com.peterscode.ecommerce_management_system.security.SecurityUtils;
import com.peterscode.ecommerce_management_system.service.AuditLogService;
import com.peterscode.ecommerce_management_system.service.AuthService;
import com.peterscode.ecommerce_management_system.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final VerificationTokenRepository verificationTokenRepository;

    @Value("${app.email.verification.expiry-hours:24}")
    private int verificationExpiryHours;

    @Value("${app.password.reset.expiry-hours:1}")
    private int passwordResetExpiryHours;

    @Override
    @Transactional

    public UserResponse registerCustomer(RegisterRequest request, HttpServletRequest httpRequest) {
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);
        log.debug("Customer registration attempt from IP: {} for email: {}", ipAddress, request.getEmail());

        if (securityUtils.isIpBlacklisted(ipAddress)) {
            auditLogService.logSecurityEvent("REGISTRATION_BLOCKED", "Blocked IP attempted registration", ipAddress);
            throw new BadRequestException("Registration is not allowed at this time");
        }

        validateGmailEmail(request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            auditLogService.logFailure("REGISTER", "USER", null, "Email already exists", httpRequest);
            throw new BadRequestException("Email is already registered");
        }

        if (!securityUtils.isPasswordStrong(request.getPassword())) {
            throw new BadRequestException("Password does not meet security requirements");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(false);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        // Fixed: Pass IP address
        String verificationToken = generateAndSaveVerificationToken(savedUser, TokenType.EMAIL_VERIFICATION, ipAddress);
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstName(), verificationToken);

        log.info("Customer registered successfully (pending verification): {}", savedUser.getEmail());
        auditLogService.logSuccess("REGISTER_CUSTOMER", "USER", savedUser.getId().toString(),
                "New customer registered - verification email sent", httpRequest);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse registerUserByAdmin(RegisterRequest request, String role, HttpServletRequest httpRequest) {
        String currentUserEmail = securityUtils.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        User adminUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (adminUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only administrators can register other users");
        }

        String ipAddress = securityUtils.getClientIpAddress(httpRequest);

        validateGmailEmail(request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        Role userRole;
        try {
            userRole = Role.valueOf(role.toUpperCase());
            if (userRole == Role.CUSTOMER) {
                throw new BadRequestException("Customers must register through public registration");
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role specified");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);
        user.setEnabled(false);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        // Fixed: Pass IP address
        String verificationToken = generateAndSaveVerificationToken(savedUser, TokenType.EMAIL_VERIFICATION, ipAddress);
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstName(), verificationToken);

        log.info("User registered by admin {}: {} with role: {}", adminUser.getEmail(), savedUser.getEmail(), userRole);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.debug("Login attempt from IP: {} for email: {}", ipAddress, request.getEmail());

        // Check if IP is blacklisted
        if (securityUtils.isIpBlacklisted(ipAddress)) {
            auditLogService.logSecurityEvent("LOGIN_BLOCKED", "Blocked IP attempted login", ipAddress);
            throw new BadRequestException("Access denied");
        }

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    auditLogService.logAuthenticationAttempt(request.getEmail(), false, ipAddress, userAgent);
                    return new UnauthorizedException("Invalid email or password");
                });

        // Check if account is locked
        if (securityUtils.isAccountLocked(request.getEmail())) {
            log.warn("Login attempt on locked account: {}", request.getEmail());
            auditLogService.logAuthenticationAttempt(request.getEmail(), false, ipAddress, userAgent);
            throw new BadRequestException("Account is temporarily locked. Please try again later.");
        }

        // Check if email is verified
        if (!user.isEmailVerified()) {
            log.warn("Login attempt with unverified email: {}", request.getEmail());
            auditLogService.logAuthenticationAttempt(request.getEmail(), false, ipAddress, userAgent);
            throw new BadRequestException("Please verify your email before logging in");
        }

        // Check if account is enabled
        if (!user.isEnabled()) {
            log.warn("Login attempt on disabled account: {}", request.getEmail());
            auditLogService.logAuthenticationAttempt(request.getEmail(), false, ipAddress, userAgent);
            throw new BadRequestException("Account is disabled. Please contact support.");
        }

        // Check login attempts
        if (securityUtils.isSuspiciousActivity(ipAddress, request.getEmail())) {
            log.warn("Suspicious login activity detected for: {} from IP: {}", request.getEmail(), ipAddress);
            securityUtils.lockAccount(request.getEmail());
            auditLogService.logSecurityEvent("SUSPICIOUS_LOGIN",
                    "Account locked due to suspicious activity", ipAddress);
            throw new BadRequestException("Too many failed login attempts. Account locked for 30 minutes.");
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(authentication, ipAddress);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            UserResponse userResponse = userMapper.toResponse(user);

            // Reset login attempts on successful login
            securityUtils.resetLoginAttempts(request.getEmail(), ipAddress);

            // Log successful login
            log.info("User logged in successfully: {}", request.getEmail());
            auditLogService.logAuthenticationAttempt(request.getEmail(), true, ipAddress, userAgent);

            // Send login notification if new device/location
            emailService.sendLoginNotification(user.getEmail(), user.getFirstName(), ipAddress);

            return new LoginResponse(accessToken, refreshToken, userResponse);

        } catch (AuthenticationException e) {
            // Record failed login attempt
            securityUtils.recordLoginAttempt(request.getEmail(), ipAddress);

            log.warn("Login failed for: {} from IP: {}", request.getEmail(), ipAddress);
            auditLogService.logAuthenticationAttempt(request.getEmail(), false, ipAddress, userAgent);

            throw new UnauthorizedException("Invalid email or password");
        }
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest httpRequest) {
        String email = securityUtils.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("No authenticated user found"));

        String ipAddress = securityUtils.getClientIpAddress(httpRequest);
        String token = getTokenFromRequest(httpRequest);

        if (token != null) {
            // Invalidate token
            jwtTokenProvider.invalidateToken(token);
        }

        // Clear security context
        SecurityContextHolder.clearContext();

        log.info("User logged out: {}", email);
        auditLogService.logLogout(email, ipAddress);
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshToken, HttpServletRequest httpRequest) {
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);

        log.debug("Token refresh attempt from IP: {}", ipAddress);

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken, ipAddress)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user is enabled and verified
        if (!user.isEnabled() || !user.isEmailVerified()) {
            throw new UnauthorizedException("Account is not active");
        }

        // Create authentication
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()
        );

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication, ipAddress);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        UserResponse userResponse = userMapper.toResponse(user);

        log.info("Token refreshed for user: {}", username);

        return new LoginResponse(newAccessToken, newRefreshToken, userResponse);
    }

    @Override
    @Transactional

    public void verifyEmail(String token, HttpServletRequest httpRequest) {
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);

        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        // FIXED: Use isExpired() instead of getExpiryDate()
        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new BadRequestException("Verification token has expired. Please request a new one.");
        }

        if (verificationToken.getTokenType() != TokenType.EMAIL_VERIFICATION) {
            throw new BadRequestException("Invalid token type");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);

        verificationTokenRepository.delete(verificationToken);
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());

        auditLogService.logSuccess("EMAIL_VERIFICATION", "USER", user.getId().toString(),
                "Email verified successfully", httpRequest);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email, HttpServletRequest httpRequest) {
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        verificationTokenRepository.deleteByUserAndTokenType(user, TokenType.EMAIL_VERIFICATION);

        // Fixed: Pass IP address
        String verificationToken = generateAndSaveVerificationToken(user, TokenType.EMAIL_VERIFICATION, ipAddress);
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationToken);

        auditLogService.logSuccess("RESEND_VERIFICATION", "USER", user.getId().toString(),
                "Verification email resent", httpRequest);
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email, HttpServletRequest httpRequest) {
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("Password reset requested for non-existent email: {}", email);
                    return new BadRequestException("If the email exists, a reset link will be sent");
                });

        if (!user.isEmailVerified() || !user.isEnabled()) {
            throw new BadRequestException("Account is not active");
        }

        verificationTokenRepository.deleteByUserAndTokenType(user, TokenType.PASSWORD_RESET);

        // Fixed: Pass IP address
        String resetToken = generateAndSaveVerificationToken(user, TokenType.PASSWORD_RESET, ipAddress);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetToken);

        auditLogService.logSuccess("PASSWORD_RESET_REQUEST", "USER", user.getId().toString(),
                "Password reset email sent", httpRequest);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword, HttpServletRequest httpRequest) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid password reset token"));

        // FIXED: Use isExpired()
        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        if (verificationToken.getTokenType() != TokenType.PASSWORD_RESET) {
            throw new BadRequestException("Invalid token type");
        }

        User user = verificationToken.getUser();

        if (!securityUtils.isPasswordStrong(newPassword)) {
            throw new BadRequestException("Password does not meet security requirements");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        verificationTokenRepository.delete(verificationToken);
        emailService.sendPasswordChangeNotification(user.getEmail(), user.getFirstName());

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    /**
     * Validate that email is a Gmail address
     */
    private void validateGmailEmail(String email) {
        if (email == null || !email.toLowerCase().endsWith("@gmail.com")) {
            throw new BadRequestException("Only @gmail.com email addresses are accepted");
        }
    }

    /**
     * Generate and save verification token
     */
    private String generateAndSaveVerificationToken(User user, TokenType tokenType, String ipAddress) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(
                tokenType == TokenType.EMAIL_VERIFICATION ? verificationExpiryHours : passwordResetExpiryHours
        );

        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .tokenType(tokenType)
                .expiresAt(expiryDate)
                .ipAddress(ipAddress) // Capture IP
                .build();

        verificationTokenRepository.save(verificationToken);
        return token;
    }

    /**
     * Extract JWT token from request
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.HEADER_STRING);
        if (bearerToken != null && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
}