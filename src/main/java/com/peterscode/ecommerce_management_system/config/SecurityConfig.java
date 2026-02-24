package com.peterscode.ecommerce_management_system.config;

import com.peterscode.ecommerce_management_system.constant.SecurityConstants;
import com.peterscode.ecommerce_management_system.security.JwtAuthenticationEntryPoint;
import com.peterscode.ecommerce_management_system.security.JwtAuthenticationFilter;
import com.peterscode.ecommerce_management_system.security.JwtTokenProvider;
import com.peterscode.ecommerce_management_system.security.RateLimitingFilter;
import com.peterscode.ecommerce_management_system.security.RequestIdFilter;
import com.peterscode.ecommerce_management_system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security Configuration
 *
 * Implements:
 * - JWT-based authentication
 * - Role-based authorization (CUSTOMER, ADMIN, SELLER, SUPPORT)
 * - CORS configuration
 * - Security headers
 * - Public endpoint access
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityUtils securityUtils;
    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== FILTER BEANS ====================

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, securityUtils);
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter() {
        return new RateLimitingFilter(redisTemplate, securityUtils);
    }

    @Bean
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    // ==================== AUTH BEANS ====================

    /**
     * Password encoder - BCrypt with strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication provider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(SecurityConstants.ALLOWED_ORIGINS));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ==================== SECURITY FILTER CHAIN ====================

    /**
     * Security filter chain - Main security configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (using JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Exception handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // Session management - Stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // ==================== PUBLIC ENDPOINTS ====================

                        // Authentication endpoints
                        .requestMatchers(
                                "/api/v1/auth/register/**",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh-token",
                                "/api/v1/auth/verify-email/**",
                                "/api/v1/auth/resend-verification",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/admin/init"
                        ).permitAll()

                        // Swagger UI / API Docs / Health
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Products & Categories (Public viewing)
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        // Inventory (Public stock checking)
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/**").permitAll()

                        // Guest Cart Operations
                        .requestMatchers("/api/v1/cart/guest/**").permitAll()

                        // Reviews (Public viewing)
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()

                        // Flash Deals (Public viewing)
                        .requestMatchers(HttpMethod.GET, "/api/v1/flash-deals/**").permitAll()

                        // Coupon Validation (Public)
                        .requestMatchers(HttpMethod.GET, "/api/v1/coupons/validate").permitAll()

                        // Product Variants (Public viewing)
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/*/variants").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/*/variants/*").permitAll()

                        // ==================== M-PESA CALLBACKS ====================
                        // CRITICAL: Must be public for Safaricom to send callbacks

                        .requestMatchers("/api/v1/payments/mpesa/callback").permitAll()
                        .requestMatchers("/api/v1/payments/mpesa/timeout").permitAll()

                        // ==================== ADMIN ONLY ====================
                        // User Management
                        .requestMatchers("/api/v1/auth/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/admin/**").hasRole("ADMIN")

                        // Reports & Analytics
                        .requestMatchers("/api/v1/reports/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/analytics/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")

                        // Order Management (Admin functions)
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/user/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/status/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*/tracking").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*/admin-notes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/**").hasRole("ADMIN")

                        // Payment Management (Admin functions)
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/*/refund").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/mpesa/query/**").hasRole("ADMIN")

                        // Shipping Management
                        .requestMatchers("/api/v1/shipping/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/shipping/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/shipping").hasRole("ADMIN")

                        // Coupon Management (Admin)
                        .requestMatchers(HttpMethod.POST, "/api/v1/coupons").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/coupons/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/coupons/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/coupons").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/coupons/active").hasRole("ADMIN")

                        // Flash Deal Management (Admin)
                        .requestMatchers(HttpMethod.POST, "/api/v1/flash-deals").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/flash-deals/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/flash-deals/**").hasRole("ADMIN")

                        // Review Management (Admin)
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/pending").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/*/reject").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/*/respond").hasRole("ADMIN")

                        // ==================== ADMIN & SELLER ====================
                        // Product Management (Create/Update/Delete)
                        .requestMatchers(HttpMethod.POST, "/api/v1/products").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasAnyRole("ADMIN", "SELLER")

                        // Category Management
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasAnyRole("ADMIN", "SELLER")

                        // Inventory Management
                        .requestMatchers(HttpMethod.PUT, "/api/v1/inventory/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/inventory/*/restock").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/inventory/*/reserve").hasAnyRole("ADMIN", "SELLER", "SYSTEM")
                        .requestMatchers(HttpMethod.POST, "/api/v1/inventory/*/release").hasAnyRole("ADMIN", "SELLER", "SYSTEM")
                        .requestMatchers(HttpMethod.POST, "/api/v1/inventory/*/confirm").hasAnyRole("ADMIN", "SELLER", "SYSTEM")
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/low-stock").hasAnyRole("ADMIN", "SELLER")

                        // Product Variant Management (Admin/Seller)
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/*/variants").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/*/variants/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/*/variants/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/*/variants/all").hasAnyRole("ADMIN", "SELLER")

                        // ==================== CUSTOMER ====================
                        // Cart Management (includes Kilimall features: selection, wishlist, save-for-later)
                        .requestMatchers("/api/v1/cart/**").hasRole("CUSTOMER")

                        // Wishlist Management
                        .requestMatchers("/api/v1/wishlist/**").hasRole("CUSTOMER")

                        // Recently Viewed
                        .requestMatchers("/api/v1/recently-viewed/**").hasRole("CUSTOMER")

                        // Address Management
                        .requestMatchers("/api/v1/addresses/**").hasRole("CUSTOMER")

                        // Order Creation & Management (includes Kilimall checkout)
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/checkout").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/my-orders/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/user/date-range").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/user/stats/**").hasRole("CUSTOMER")

                        // Payment Initiation
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/initiate").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/user").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/order/**").hasRole("CUSTOMER")

                        // Review Creation
                        .requestMatchers(HttpMethod.POST, "/api/v1/reviews/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/**").hasRole("CUSTOMER")

                        // ==================== AUTHENTICATED ====================
                        // Require authentication but allow any role

                        .requestMatchers("/api/v1/users/profile/**").authenticated()
                        .requestMatchers("/api/v1/notifications/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/shipping/**").authenticated()

                        // ==================== DEFAULT ====================
                        // Catch-all: require authentication
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())
                // Filter execution order (last addFilterBefore runs first):
                // 1. requestIdFilter → 2. rateLimitingFilter → 3. jwtAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestIdFilter(), UsernamePasswordAuthenticationFilter.class);

        // ==================== SECURITY HEADERS ====================
        http.headers(headers -> headers
                // Content Security Policy
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                "style-src 'self' 'unsafe-inline'; " +
                                "img-src 'self' data: https:; " +
                                "font-src 'self' data:; " +
                                "connect-src 'self'; " +
                                "frame-ancestors 'none'; " +
                                "form-action 'self'")
                )

                // Prevent clickjacking
                .frameOptions(frame -> frame.deny())

                // XSS Protection
                .xssProtection(xss -> xss
                        .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))

                // HSTS - Force HTTPS
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000) // 1 year
                )

                // Prevent MIME type sniffing
                .contentTypeOptions(contentType -> {})

                // Referrer Policy
                .referrerPolicy(referrer -> referrer
                        .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
        );

        return http.build();
    }
}

