package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    @Async
    @Override
    public void sendVerificationEmail(String to, String username, String token) {
        try {

            String verificationLink = frontendUrl + "/api/v1/auth/verify-email?token=" + token;

            String subject = "Verify Your Email Address - E-commerce Platform";
            String body = buildEmailTemplate(
                    username,
                    "Email Verification Required",
                    "Thank you for registering with our E-commerce Platform. Please verify your email address to activate your account.",
                    "Click the button below to verify your email address. This link will expire in 24 hours.",
                    verificationLink,
                    "Verify Email Address"
            );

            sendHtmlEmail(to, subject, body);
            log.info("Verification email sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", to, e);
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String username, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            String subject = "Password Reset Request - E-commerce Platform";
            String body = buildEmailTemplate(
                    username,
                    "Password Reset Request",
                    "We received a request to reset your password. If you didn't make this request, please ignore this email.",
                    "Click the button below to reset your password. This link will expire in 1 hour.",
                    resetLink,
                    "Reset Password"
            );

            sendHtmlEmail(to, subject, body);
            log.info("Password reset email sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
        }
    }

    @Async
    @Override
    public void sendWelcomeEmail(String to, String username) {
        try {
            String subject = "Welcome to E-commerce Platform!";
            String body = buildEmailTemplate(
                    username,
                    "Welcome to Our Platform!",
                    "Your account has been successfully created and verified. You can now start shopping!",
                    "We're excited to have you on board. Explore our products and enjoy exclusive deals.",
                    frontendUrl + "/products",
                    "Start Shopping"
            );

            sendHtmlEmail(to, subject, body);
            log.info("Welcome email sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }

    @Async
    @Override
    public void sendLoginNotification(String to, String username, String ipAddress) {
        try {
            String loginTime = LocalDateTime.now().format(formatter);

            String subject = "New Login to Your Account - E-commerce Platform";
            String body = buildNotificationTemplate(
                    username,
                    "New Login Detected",
                    "We detected a new login to your account. If this was you, you can safely ignore this email.",
                    "Login Details:",
                    "Time: " + loginTime + "<br>" +
                            "IP Address: " + ipAddress, // Removed undefined 'location'
                    "If you don't recognize this activity, please reset your password immediately."
            );

            sendHtmlEmail(to, subject, body);
            log.info("Login notification sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send login notification to: {}", to, e);
        }
    }

    @Async
    @Override
    public void sendPasswordChangeNotification(String to, String username) {
        try {
            String changeTime = LocalDateTime.now().format(formatter);

            String subject = "Password Changed Successfully - E-commerce Platform";
            String body = buildNotificationTemplate(
                    username,
                    "Password Changed",
                    "Your password has been changed successfully.",
                    "Change Details:",
                    "Time: " + changeTime,
                    "If you didn't make this change, please contact our support team immediately."
            );

            sendHtmlEmail(to, subject, body);
            log.info("Password change notification sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send password change notification to: {}", to, e);
        }
    }

    @Async
    @Override
    public void sendAccountLockedNotification(String to, String username, String reason) {
        try {
            String lockTime = LocalDateTime.now().format(formatter);

            String subject = "Account Temporarily Locked - E-commerce Platform";
            String body = buildNotificationTemplate(
                    username,
                    "Account Locked",
                    "Your account has been temporarily locked for security reasons.",
                    "Lock Details:",
                    "Time: " + lockTime + "<br>" +
                            "Reason: " + reason,
                    "Your account will be automatically unlocked in 30 minutes. If you need immediate assistance, please contact support."
            );

            sendHtmlEmail(to, subject, body);
            log.info("Account locked notification sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send account locked notification to: {}", to, e);
        }
    }

    @Async
    @Override
    public void sendTwoFactorCode(String to, String code, String username) {
        try {
            String subject = "Your Two-Factor Authentication Code - E-commerce Platform";
            String body = buildNotificationTemplate(
                    username,
                    "Two-Factor Authentication",
                    "You requested a two-factor authentication code. Use the code below to complete your login.",
                    "Your Code:",
                    "<div style='font-size: 32px; font-weight: bold; color: #4CAF50; text-align: center; padding: 20px; background: #f5f5f5; border-radius: 8px;'>" + code + "</div>",
                    "This code will expire in 10 minutes. If you didn't request this code, please contact support immediately."
            );

            sendHtmlEmail(to, subject, body);
            log.info("Two-factor code sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send two-factor code to: {}", to, e);
        }
    }

    @Override
    public boolean isValidGmailAddress(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        email = email.toLowerCase().trim();

        // Check if email ends with @gmail.com
        if (!email.endsWith("@gmail.com")) {
            log.warn("Invalid email domain: {}. Only Gmail addresses are allowed.", email);
            return false;
        }

        // Basic email format validation
        String emailRegex = "^[a-zA-Z0-9._%+-]+@gmail\\.com$";
        boolean isValid = email.matches(emailRegex);

        if (!isValid) {
            log.warn("Invalid Gmail format: {}", email);
        }

        return isValid;
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(message);
    }

    private String buildEmailTemplate(String username, String title, String subtitle,
                                      String message, String actionUrl, String actionText) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;'>" +
                "<div style='max-width: 600px; margin: 20px auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center;'>" +
                "<h1 style='color: white; margin: 0; font-size: 28px;'>E-commerce Platform</h1>" +
                "</div>" +
                "<div style='padding: 40px 30px;'>" +
                "<h2 style='color: #333; margin-top: 0;'>Hello " + username + ",</h2>" +
                "<h3 style='color: #555; font-weight: normal;'>" + title + "</h3>" +
                "<p style='color: #666; line-height: 1.6; font-size: 16px;'>" + subtitle + "</p>" +
                "<p style='color: #666; line-height: 1.6; font-size: 16px;'>" + message + "</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='" + actionUrl + "' style='display: inline-block; padding: 14px 40px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px;'>" +
                actionText +
                "</a>" +
                "</div>" +
                "<p style='color: #999; font-size: 14px; margin-top: 30px;'>If the button doesn't work, copy and paste this link into your browser:</p>" +
                "<p style='color: #667eea; font-size: 14px; word-break: break-all;'>" + actionUrl + "</p>" +
                "</div>" +
                "<div style='background: #f8f8f8; padding: 20px; text-align: center; border-top: 1px solid #eee;'>" +
                "<p style='color: #999; font-size: 12px; margin: 0;'>© 2026 E-commerce Platform. All rights reserved.</p>" +
                "<p style='color: #999; font-size: 12px; margin: 10px 0 0 0;'>This is an automated message, please do not reply.</p>" +
                "</div>" +
                "</div>" +
                "</body></html>";
    }

    private String buildNotificationTemplate(String username, String title, String subtitle,
                                             String detailsTitle, String details, String footer) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;'>" +
                "<div style='max-width: 600px; margin: 20px auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center;'>" +
                "<h1 style='color: white; margin: 0; font-size: 28px;'>E-commerce Platform</h1>" +
                "</div>" +
                "<div style='padding: 40px 30px;'>" +
                "<h2 style='color: #333; margin-top: 0;'>Hello " + username + ",</h2>" +
                "<h3 style='color: #555; font-weight: normal;'>" + title + "</h3>" +
                "<p style='color: #666; line-height: 1.6; font-size: 16px;'>" + subtitle + "</p>" +
                "<div style='background: #f8f8f8; padding: 20px; border-radius: 6px; margin: 20px 0;'>" +
                "<h4 style='margin-top: 0; color: #333;'>" + detailsTitle + "</h4>" +
                "<p style='color: #666; line-height: 1.6; margin: 0;'>" + details + "</p>" +
                "</div>" +
                "<p style='color: #666; line-height: 1.6; font-size: 14px; margin-top: 20px;'>" + footer + "</p>" +
                "</div>" +
                "<div style='background: #f8f8f8; padding: 20px; text-align: center; border-top: 1px solid #eee;'>" +
                "<p style='color: #999; font-size: 12px; margin: 0;'>© 2026 E-commerce Platform. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body></html>";
    }
}