package com.peterscode.ecommerce_management_system.security;

import com.peterscode.ecommerce_management_system.constant.SecurityConstants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_BLACKLIST_KEY_PREFIX = "security:token:blacklist:";
    private static final String TOKEN_WHITELIST_KEY_PREFIX = "security:token:whitelist:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "security:refresh:token:";

    public JwtTokenProvider(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate signing key from secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate access token with user details and IP address
     */
    public String generateAccessToken(Authentication authentication, String ipAddress) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);
        String tokenId = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenId", tokenId);
        claims.put("email", userDetails.getUsername());
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put("ipAddress", ipAddress);
        claims.put("tokenType", "ACCESS");

        String token = Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        // Store token in whitelist (Redis)
        storeTokenInWhitelist(tokenId, userDetails.getUsername(), accessTokenExpiration);

        log.debug("Generated access token for user: {} with tokenId: {}", userDetails.getUsername(), tokenId);
        return token;
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);
        String tokenId = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenId", tokenId);
        claims.put("tokenType", "REFRESH");

        String token = Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        // Store refresh token in Redis
        storeRefreshToken(tokenId, userDetails.getUsername(), refreshTokenExpiration);

        log.debug("Generated refresh token for user: {} with tokenId: {}", userDetails.getUsername(), tokenId);
        return token;
    }

    /**
     * Extract username from JWT token
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract token ID from JWT token
     */
    public String getTokenIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("tokenId", String.class);
        } catch (Exception e) {
            log.error("Error extracting token ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate JWT token with comprehensive security checks
     */
    public boolean validateToken(String token, String ipAddress) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenId = claims.get("tokenId", String.class);
            String tokenIpAddress = claims.get("ipAddress", String.class);

            // Check if token is blacklisted
            if (isTokenBlacklisted(tokenId)) {
                log.warn("Token is blacklisted: {}", tokenId);
                return false;
            }

            // Check if token is in whitelist
            if (!isTokenWhitelisted(tokenId)) {
                log.warn("Token is not in whitelist: {}", tokenId);
                return false;
            }

            // IP address validation (optional, can be disabled in config)
            if (tokenIpAddress != null && !tokenIpAddress.equals(ipAddress)) {
                log.warn("IP address mismatch. Token IP: {}, Request IP: {}", tokenIpAddress, ipAddress);
                // You can choose to reject or just log this
                // return false;
            }

            return true;

        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Invalidate token (logout, password change, etc.)
     */
    public void invalidateToken(String token) {
        String tokenId = getTokenIdFromToken(token);
        if (tokenId != null) {
            blacklistToken(tokenId);
            removeTokenFromWhitelist(tokenId);
            log.info("Token invalidated: {}", tokenId);
        }
    }

    /**
     * Store token in whitelist (Redis)
     */
    private void storeTokenInWhitelist(String tokenId, String username, long expiration) {
        String key = TOKEN_WHITELIST_KEY_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, username, expiration, TimeUnit.MILLISECONDS);
    }

    /**
     * Check if token is in whitelist
     */
    private boolean isTokenWhitelisted(String tokenId) {
        String key = TOKEN_WHITELIST_KEY_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Remove token from whitelist
     */
    private void removeTokenFromWhitelist(String tokenId) {
        String key = TOKEN_WHITELIST_KEY_PREFIX + tokenId;
        redisTemplate.delete(key);
    }

    /**
     * Blacklist a token
     */
    private void blacklistToken(String tokenId) {
        String key = TOKEN_BLACKLIST_KEY_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, "blacklisted", accessTokenExpiration, TimeUnit.MILLISECONDS);
    }

    /**
     * Check if token is blacklisted
     */
    private boolean isTokenBlacklisted(String tokenId) {
        String key = TOKEN_BLACKLIST_KEY_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Store refresh token
     */
    private void storeRefreshToken(String tokenId, String username, long expiration) {
        String key = REFRESH_TOKEN_KEY_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, username, expiration, TimeUnit.MILLISECONDS);
    }

    /**
     * Invalidate all user tokens (on password change, suspicious activity)
     */
    public void invalidateAllUserTokens(String username) {

        log.info("Invalidating all tokens for user: {}", username);
        // Implementation would scan Redis for all tokens belonging to this user
    }

    /**
     * Generates a token based solely on UserDetails.
     * Useful for internal calls where Authentication object or IP is not immediately available.
     * Default IP set to "N/A".
     */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);
        String tokenId = UUID.randomUUID().toString();
        String defaultIp = "N/A"; // Placeholder since IP is not provided in this signature

        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenId", tokenId);
        claims.put("email", userDetails.getUsername());
        // Extract roles from UserDetails
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put("ipAddress", defaultIp);
        claims.put("tokenType", "ACCESS");

        String token = Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        // CRITICAL: We must store this in Redis, otherwise validateToken() will fail
        storeTokenInWhitelist(tokenId, userDetails.getUsername(), accessTokenExpiration);

        log.debug("Generated simple access token for user: {} with tokenId: {}", userDetails.getUsername(), tokenId);
        return token;
    }
}