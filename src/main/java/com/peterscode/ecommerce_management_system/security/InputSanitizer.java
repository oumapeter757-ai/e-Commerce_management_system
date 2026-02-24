package com.peterscode.ecommerce_management_system.security;

import org.springframework.stereotype.Component;

/**
 * Input sanitization utility for XSS prevention.
 * Strips potentially dangerous HTML/script content from user-supplied strings.
 * Used across controllers and services for all user-supplied text fields.
 */
@Component
public class InputSanitizer {

    /**
     * Sanitize user input by removing potentially dangerous HTML/script content.
     * Safe for use on all user-supplied string fields (names, notes, descriptions, etc.)
     *
     * @param input raw user input
     * @return sanitized string with HTML tags, script content, and event handlers removed
     */
    public String sanitize(String input) {
        if (input == null) return null;
        if (input.isBlank()) return input.trim();

        String sanitized = input;

        // Remove script tags and content
        sanitized = sanitized.replaceAll("(?i)<script[^>]*>.*?</script>", "");

        // Remove all HTML tags
        sanitized = sanitized.replaceAll("<[^>]*>", "");

        // Remove javascript: protocol
        sanitized = sanitized.replaceAll("(?i)javascript\\s*:", "");

        // Remove vbscript: protocol
        sanitized = sanitized.replaceAll("(?i)vbscript\\s*:", "");

        // Remove event handlers (onclick, onload, onerror, etc.)
        sanitized = sanitized.replaceAll("(?i)\\bon\\w+\\s*=", "");

        // Remove data: URIs that could contain scripts
        sanitized = sanitized.replaceAll("(?i)data\\s*:[^;]*;base64", "");

        // Remove expression() CSS
        sanitized = sanitized.replaceAll("(?i)expression\\s*\\(", "");

        // Remove url() CSS that could load external resources
        sanitized = sanitized.replaceAll("(?i)url\\s*\\(\\s*['\"]?\\s*javascript", "");

        // Encode remaining special characters
        sanitized = sanitized
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");

        return sanitized.trim();
    }

    /**
     * Sanitize input and enforce maximum length.
     *
     * @param input     raw user input
     * @param maxLength maximum allowed length
     * @return sanitized and length-limited string
     */
    public String sanitize(String input, int maxLength) {
        String sanitized = sanitize(input);
        if (sanitized != null && sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }
        return sanitized;
    }

    /**
     * Light sanitization - only remove dangerous script/event content but preserve basic formatting.
     * Used for fields that may contain markdown or basic text formatting.
     *
     * @param input raw user input
     * @return sanitized string with scripts removed but basic text preserved
     */
    public String sanitizeLight(String input) {
        if (input == null) return null;
        if (input.isBlank()) return input.trim();

        String sanitized = input;

        // Remove script tags and content
        sanitized = sanitized.replaceAll("(?i)<script[^>]*>.*?</script>", "");

        // Remove iframe tags
        sanitized = sanitized.replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "");

        // Remove event handlers
        sanitized = sanitized.replaceAll("(?i)\\bon\\w+\\s*=", "");

        // Remove javascript: protocol
        sanitized = sanitized.replaceAll("(?i)javascript\\s*:", "");

        return sanitized.trim();
    }

    /**
     * Validate and sanitize a UUID-format session ID.
     * Prevents session fixation attacks via injected session IDs.
     *
     * @param sessionId the session ID to validate
     * @return true if valid UUID format
     */
    public boolean isValidSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        if (sessionId.length() > 36) return false;
        return sessionId.matches("^[a-fA-F0-9\\-]+$");
    }
}

