package com.lodhi.auth.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class LoggingUtilsTest {

    @BeforeEach
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void testSetUserContext_WithValidData() {
        LoggingUtils.setUserContext(123L, "testuser");
        assertEquals("123", MDC.get(LoggingUtils.USER_ID));
        assertEquals("testuser", MDC.get(LoggingUtils.USERNAME));
    }

    @Test
    void testSetUserContext_WithNullData() {
        LoggingUtils.setUserContext(null, null);
        assertNull(MDC.get(LoggingUtils.USER_ID));
        assertNull(MDC.get(LoggingUtils.USERNAME));
    }

    @Test
    void testSetRequestContext_WithValidData() {
        LoggingUtils.setRequestContext("req-123", "192.168.1.1", "Mozilla");
        assertEquals("req-123", MDC.get(LoggingUtils.REQUEST_ID));
        assertEquals("192.168.1.1", MDC.get(LoggingUtils.IP_ADDRESS));
        assertEquals("Mozilla", MDC.get(LoggingUtils.USER_AGENT));
    }

    @Test
    void testSetRequestContext_WithNullData() {
        LoggingUtils.setRequestContext(null, null, null);
        assertNull(MDC.get(LoggingUtils.REQUEST_ID));
        assertNull(MDC.get(LoggingUtils.IP_ADDRESS));
        assertNull(MDC.get(LoggingUtils.USER_AGENT));
    }

    @Test
    void testSetEndpointContext_WithValidData() {
        LoggingUtils.setEndpointContext("/api/v1/users", "GET");
        assertEquals("/api/v1/users", MDC.get(LoggingUtils.ENDPOINT));
        assertEquals("GET", MDC.get(LoggingUtils.HTTP_METHOD));
    }

    @Test
    void testSetEndpointContext_WithNullData() {
        LoggingUtils.setEndpointContext(null, null);
        assertNull(MDC.get(LoggingUtils.ENDPOINT));
        assertNull(MDC.get(LoggingUtils.HTTP_METHOD));
    }

    @Test
    void testSetSessionId_WithValidData() {
        LoggingUtils.setSessionId("sess-123");
        assertEquals("sess-123", MDC.get(LoggingUtils.SESSION_ID));
    }

    @Test
    void testSetSessionId_WithNullData() {
        LoggingUtils.setSessionId(null);
        assertNull(MDC.get(LoggingUtils.SESSION_ID));
    }

    @Test
    void testClearUserContext() {
        MDC.put(LoggingUtils.USER_ID, "123");
        MDC.put(LoggingUtils.USERNAME, "testuser");
        MDC.put(LoggingUtils.REQUEST_ID, "req-123"); // Should remain
        
        LoggingUtils.clearUserContext();
        
        assertNull(MDC.get(LoggingUtils.USER_ID));
        assertNull(MDC.get(LoggingUtils.USERNAME));
        assertEquals("req-123", MDC.get(LoggingUtils.REQUEST_ID));
    }

    @Test
    void testClearRequestContext() {
        MDC.put(LoggingUtils.REQUEST_ID, "req-123");
        MDC.put(LoggingUtils.IP_ADDRESS, "192.168.1.1");
        MDC.put(LoggingUtils.USER_AGENT, "Mozilla");
        MDC.put(LoggingUtils.ENDPOINT, "/api/v1");
        MDC.put(LoggingUtils.HTTP_METHOD, "POST");
        MDC.put(LoggingUtils.USER_ID, "123"); // Should remain
        
        LoggingUtils.clearRequestContext();
        
        assertNull(MDC.get(LoggingUtils.REQUEST_ID));
        assertNull(MDC.get(LoggingUtils.IP_ADDRESS));
        assertNull(MDC.get(LoggingUtils.USER_AGENT));
        assertNull(MDC.get(LoggingUtils.ENDPOINT));
        assertNull(MDC.get(LoggingUtils.HTTP_METHOD));
        assertEquals("123", MDC.get(LoggingUtils.USER_ID));
    }

    @Test
    void testClearAll() {
        MDC.put(LoggingUtils.USER_ID, "123");
        MDC.put(LoggingUtils.REQUEST_ID, "req-123");
        
        LoggingUtils.clearAll();
        
        assertNull(MDC.get(LoggingUtils.USER_ID));
        assertNull(MDC.get(LoggingUtils.REQUEST_ID));
    }
}
