package com.lodhi.auth.utils;

import org.slf4j.MDC;

/**
 * Utility class for accessing correlation ID from anywhere in the application.
 * The correlation ID is stored in MDC by LoggingFilter and can be retrieved
 * for including in external API calls, error messages, etc.
 */
public class CorrelationIdUtils {

    private static final String CORRELATION_ID_KEY = "requestId";

    /**
     * Get the current correlation ID from MDC.
     * Returns null if no correlation ID is set (e.g., outside of HTTP request context).
     * 
     * @return Current correlation ID, or null if not available
     */
    public static String getCurrentCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    /**
     * Check if a correlation ID is currently set in MDC.
     * 
     * @return true if correlation ID exists, false otherwise
     */
    public static boolean hasCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY) != null;
    }

    /**
     * Get correlation ID with a fallback value.
     * Useful when correlation ID is required but might not be available.
     * 
     * @param fallback Fallback value if no correlation ID is set
     * @return Current correlation ID, or fallback if not available
     */
    public static String getOrDefault(String fallback) {
        String correlationId = getCurrentCorrelationId();
        return correlationId != null ? correlationId : fallback;
    }
}
