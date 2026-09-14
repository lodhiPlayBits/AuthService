package com.lodhi.auth.filters;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.lodhi.auth.utils.LoggingUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter to add request correlation ID and context to MDC for structured logging and distributed tracing.
 * 
 * Correlation ID Strategy:
 * 1. Accept incoming correlation ID from standard headers (X-Request-ID, X-Correlation-ID, etc.)
 * 2. Generate new UUID if no correlation ID provided
 * 3. Add to MDC for logging
 * 4. Return in response headers for client-side tracing
 * 
 * This enables end-to-end request tracing across multiple services.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    // Standard correlation ID header names (in order of preference)
    private static final String[] CORRELATION_ID_HEADERS = {
        "X-Request-ID",       // Most common
        "X-Correlation-ID",   // Also common in microservices
        "X-Trace-ID",         // OpenTelemetry style
        "Request-ID",         // Alternative
        "RequestId"           // Alternative without hyphen
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Try to extract existing correlation ID from incoming request
            String correlationId = extractCorrelationId(request);
            
            // Generate new correlation ID if none provided
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Using incoming correlation ID: {}", correlationId);
            }
            
            // Extract request information
            String ipAddress = extractIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String endpoint = request.getRequestURI();
            String httpMethod = request.getMethod();
            
            // Add to MDC for all subsequent logs
            LoggingUtils.setRequestContext(correlationId, ipAddress, userAgent);
            LoggingUtils.setEndpointContext(endpoint, httpMethod);
            
            // Add correlation ID to response headers (multiple standard names for compatibility)
            response.setHeader("X-Request-ID", correlationId);
            response.setHeader("X-Correlation-ID", correlationId);
            
            log.debug("Incoming request: {} {} [correlation-id: {}]", httpMethod, endpoint, correlationId);
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
            log.debug("Completed request: {} {} - Status: {} [correlation-id: {}]", 
                     httpMethod, endpoint, response.getStatus(), correlationId);
            
        } finally {
            // Always clear MDC to prevent memory leaks
            LoggingUtils.clearAll();
        }
    }

    /**
     * Extract correlation ID from incoming request headers.
     * Checks multiple standard header names in order of preference.
     * 
     * @param request HTTP request
     * @return Correlation ID if found, null otherwise
     */
    private String extractCorrelationId(HttpServletRequest request) {
        for (String headerName : CORRELATION_ID_HEADERS) {
            String value = request.getHeader(headerName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Extract client IP address, considering proxy headers.
     */
    private String extractIpAddress(HttpServletRequest request) {
        // Check for proxy headers
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return ip.split(",")[0].trim();
        }
        
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        // Fall back to remote address
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip actuator endpoints to reduce noise
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }
}
