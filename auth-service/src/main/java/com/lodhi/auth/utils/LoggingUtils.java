package com.lodhi.auth.utils;

import org.slf4j.MDC;

/**
 * Utility class for structured logging with MDC (Mapped Diagnostic Context).
 * MDC allows adding contextual information to logs that will be included in all log statements.
 * 
 * Use this to add request-specific context like userId, traceId, etc.
 */
public class LoggingUtils {

    // MDC keys
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String REQUEST_ID = "requestId";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String USER_AGENT = "userAgent";
    public static final String ENDPOINT = "endpoint";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String SESSION_ID = "sessionId";
    
    // Constants
    private static final String UNKNOWN = "unknown";

    public static String extractClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !UNKNOWN.equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !UNKNOWN.equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static String sanitizeRequestId(String requestId) {
        if (requestId == null) return null;
        String clean = requestId.replaceAll("[^A-Za-z0-9-]", "");
        return clean.length() > 50 ? clean.substring(0, 50) : clean;
    }

    public static String maskIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return UNKNOWN;
        if (identifier.contains("@")) {
            String[] parts = identifier.split("@", 2);
            String name = parts[0];
            if (name.length() <= 2) return "***@" + parts[1];
            return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + parts[1];
        }
        if (identifier.length() <= 2) return "***";
        return identifier.charAt(0) + "***" + identifier.charAt(identifier.length() - 1);
    }
    /**
     * Add user context to MDC for all subsequent logs in this thread.
     * Call this after successful authentication.
     * 
     * @param userId User ID
     * @param username Username or email
     */
    public static void setUserContext(Long userId, String username) {
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
        if (username != null) {
            MDC.put(USERNAME, username);
        }
    }

    /**
     * Add request context to MDC.
     * Call this at the beginning of request processing.
     * 
     * @param requestId Unique request ID
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     */
    public static void setRequestContext(String requestId, String ipAddress, String userAgent) {
        if (requestId != null) {
            MDC.put(REQUEST_ID, requestId);
        }
        if (ipAddress != null) {
            MDC.put(IP_ADDRESS, ipAddress);
        }
        if (userAgent != null) {
            MDC.put(USER_AGENT, userAgent);
        }
    }

    /**
     * Add endpoint context to MDC.
     * 
     * @param endpoint Request endpoint/path
     * @param httpMethod HTTP method (GET, POST, etc.)
     */
    public static void setEndpointContext(String endpoint, String httpMethod) {
        if (endpoint != null) {
            MDC.put(ENDPOINT, endpoint);
        }
        if (httpMethod != null) {
            MDC.put(HTTP_METHOD, httpMethod);
        }
    }

    /**
     * Add session context to MDC.
     * 
     * @param sessionId Session ID
     */
    public static void setSessionId(String sessionId) {
        if (sessionId != null) {
            MDC.put(SESSION_ID, sessionId);
        }
    }

    /**
     * Clear user context from MDC.
     * Call this at the end of request processing or after logout.
     */
    public static void clearUserContext() {
        MDC.remove(USER_ID);
        MDC.remove(USERNAME);
    }

    /**
     * Clear request context from MDC.
     * Call this at the end of request processing.
     */
    public static void clearRequestContext() {
        MDC.remove(REQUEST_ID);
        MDC.remove(IP_ADDRESS);
        MDC.remove(USER_AGENT);
        MDC.remove(ENDPOINT);
        MDC.remove(HTTP_METHOD);
    }

    /**
     * Clear all MDC context.
     * Call this at the end of request processing to prevent memory leaks.
     */
    public static void clearAll() {
        MDC.clear();
    }
}
