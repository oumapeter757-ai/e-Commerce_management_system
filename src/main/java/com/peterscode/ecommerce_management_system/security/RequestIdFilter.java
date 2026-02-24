package com.peterscode.ecommerce_management_system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that assigns a unique request ID to every incoming request.
 * - Reads X-Request-ID header if present (from API gateways/load balancers)
 * - Generates a UUID if not present
 * - Stores in MDC for log correlation
 * - Adds to response header for client traceability
 */
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);

        // Generate if not provided, validate format if provided
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            requestId = UUID.randomUUID().toString();
        } else {
            // Sanitize: only allow alphanumeric, hyphens, underscores
            requestId = requestId.replaceAll("[^a-zA-Z0-9\\-_]", "");
            if (requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
        }

        try {
            // Store in MDC for log correlation
            MDC.put(MDC_REQUEST_ID, requestId);

            // Add to response header
            response.setHeader(REQUEST_ID_HEADER, requestId);

            filterChain.doFilter(request, response);
        } finally {
            // Always clear MDC to prevent leaking between requests
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}

