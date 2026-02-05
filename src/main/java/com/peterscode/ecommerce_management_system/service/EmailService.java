package com.peterscode.ecommerce_management_system.service;
public interface EmailService {

    void sendVerificationEmail(String to, String username, String token); // Added username

    void sendPasswordResetEmail(String to, String username, String token); // Added username

    void sendWelcomeEmail(String to, String username); // Added username

    void sendLoginNotification(String to, String username, String ipAddress);

    void sendPasswordChangeNotification(String to, String username); // Added username

    void sendAccountLockedNotification(String to, String username, String reason);

    void sendTwoFactorCode(String to, String code, String username);

    boolean isValidGmailAddress(String email);
}