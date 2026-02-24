package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
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
        String verificationLink = frontendUrl + "/api/v1/auth/verify-email?token=" + token;

        String body = buildEmailTemplate(
                username,
                "Email Verification Required",
                "Thank you for registering. Please verify your email to activate your account.",
                "Click the button below to verify. This link expires in 24 hours.",
                verificationLink,
                "Verify Email Address"
        );

        sendHtmlEmailSafe(to, "Verify Your Email Address - E-commerce Platform", body);
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String username, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        String body = buildEmailTemplate(
                username,
                "Password Reset Request",
                "We received a request to reset your password.",
                "Click the button below to reset. This link expires in 1 hour.",
                resetLink,
                "Reset Password"
        );

        sendHtmlEmailSafe(to, "Password Reset Request - E-commerce Platform", body);
    }

    @Async
    @Override
    public void sendWelcomeEmail(String to, String username) {
        String body = buildEmailTemplate(
                username,
                "Welcome to Our Platform!",
                "Your account has been successfully verified.",
                "We're excited to have you. Explore our products and enjoy exclusive deals.",
                frontendUrl + "/products",
                "Start Shopping"
        );

        sendHtmlEmailSafe(to, "Welcome to E-commerce Platform!", body);
    }

    @Async
    @Override
    public void sendLoginNotification(String to, String username, String ipAddress) {
        String loginTime = LocalDateTime.now().format(formatter);

        String body = buildNotificationTemplate(
                username,
                "New Login Detected",
                "We detected a new login to your account.",
                "Login Details:",
                "Time: " + loginTime + "<br>IP Address: " + ipAddress,
                "If you don't recognize this activity, reset your password immediately."
        );

        sendHtmlEmailSafe(to, "New Login to Your Account", body);
    }

    @Async
    @Override
    public void sendPasswordChangeNotification(String to, String username) {
        String changeTime = LocalDateTime.now().format(formatter);

        String body = buildNotificationTemplate(
                username,
                "Password Changed",
                "Your password has been changed successfully.",
                "Change Details:",
                "Time: " + changeTime,
                "If you didn't make this change, contact support immediately."
        );

        sendHtmlEmailSafe(to, "Password Changed Successfully", body);
    }

    @Async
    @Override
    public void sendAccountLockedNotification(String to, String username, String reason) {
        String lockTime = LocalDateTime.now().format(formatter);

        String body = buildNotificationTemplate(
                username,
                "Account Locked",
                "Your account has been temporarily locked for security reasons.",
                "Lock Details:",
                "Time: " + lockTime + "<br>Reason: " + reason,
                "Your account will unlock automatically in 30 minutes."
        );

        sendHtmlEmailSafe(to, "Account Temporarily Locked", body);
    }

    @Async
    @Override
    public void sendTwoFactorCode(String to, String code, String username) {
        String codeHtml = "<div style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #4CAF50; text-align: center; padding: 20px; background: #f5f5f5; border-radius: 8px; margin: 20px 0;'>" + code + "</div>";

        String body = buildNotificationTemplate(
                username,
                "Two-Factor Authentication",
                "Use the code below to complete your login.",
                "Your Code:",
                codeHtml,
                "This code expires in 10 minutes."
        );

        sendHtmlEmailSafe(to, "Your Two-Factor Authentication Code", body);
    }

    @Override
    public boolean isValidGmailAddress(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        // Strict check: only allows actual gmail.com domain
        return email.trim().toLowerCase().endsWith("@gmail.com");
    }

    /**
     * Public method that THROWS exceptions.
     * Use this when the caller needs to know if sending failed.
     */
    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(message);
        log.info("Email sent successfully to: {}", to);
    }

    /**
     * Internal Safe method for Async calls.
     * SWALLOWS exceptions to prevent Async threads from crashing.
     */
    private void sendHtmlEmailSafe(String to, String subject, String htmlBody) {
        try {
            sendHtmlEmail(to, subject, htmlBody);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email to {}. Error: {}", to, e.getMessage());
            // Optional: Add logic here to save failed emails to a database for retry
        }
    }

    // --- HTML TEMPLATES (Java 15+ Text Blocks) ---

    private String buildEmailTemplate(String username, String title, String subtitle,
                                      String message, String actionUrl, String actionText) {
        return """
            <!DOCTYPE html>
            <html>
            <body style='font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;'>
                <div style='max-width: 600px; margin: 20px auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>
                    <div style='background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center;'>
                        <h1 style='color: white; margin: 0; font-size: 28px;'>E-commerce Platform</h1>
                    </div>
                    <div style='padding: 40px 30px;'>
                        <h2 style='color: #333; margin-top: 0;'>Hello %s,</h2>
                        <h3 style='color: #555; font-weight: normal;'>%s</h3>
                        <p style='color: #666; line-height: 1.6; font-size: 16px;'>%s</p>
                        <p style='color: #666; line-height: 1.6; font-size: 16px;'>%s</p>
                        <div style='text-align: center; margin: 30px 0;'>
                            <a href='%s' style='display: inline-block; padding: 14px 40px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px;'>
                                %s
                            </a>
                        </div>
                        <p style='color: #999; font-size: 14px; margin-top: 30px;'>If the button doesn't work, copy this link:</p>
                        <p style='color: #667eea; font-size: 14px; word-break: break-all;'>%s</p>
                    </div>
                    <div style='background: #f8f8f8; padding: 20px; text-align: center; border-top: 1px solid #eee;'>
                        <p style='color: #999; font-size: 12px; margin: 0;'>© 2026 E-commerce Platform.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, title, subtitle, message, actionUrl, actionText, actionUrl);
    }

    private String buildNotificationTemplate(String username, String title, String subtitle,
                                             String detailsTitle, String detailsHtml, String footer) {
        return """
            <!DOCTYPE html>
            <html>
            <body style='font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;'>
                <div style='max-width: 600px; margin: 20px auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>
                    <div style='background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center;'>
                        <h1 style='color: white; margin: 0; font-size: 28px;'>E-commerce Platform</h1>
                    </div>
                    <div style='padding: 40px 30px;'>
                        <h2 style='color: #333; margin-top: 0;'>Hello %s,</h2>
                        <h3 style='color: #555; font-weight: normal;'>%s</h3>
                        <p style='color: #666; line-height: 1.6; font-size: 16px;'>%s</p>
                        <div style='background: #f8f8f8; padding: 20px; border-radius: 6px; margin: 20px 0;'>
                            <h4 style='margin-top: 0; color: #333;'>%s</h4>
                            <div style='color: #666; line-height: 1.6; margin: 0;'>%s</div>
                        </div>
                        <p style='color: #666; line-height: 1.6; font-size: 14px; margin-top: 20px;'>%s</p>
                    </div>
                    <div style='background: #f8f8f8; padding: 20px; text-align: center; border-top: 1px solid #eee;'>
                        <p style='color: #999; font-size: 12px; margin: 0;'>© 2026 E-commerce Platform.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, title, subtitle, detailsTitle, detailsHtml, footer);
    }
}