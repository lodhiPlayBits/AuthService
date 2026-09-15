package com.lodhi.auth.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationIdUtilsTest {

    @BeforeEach
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void testGetCurrentCorrelationId_WhenSet() {
        MDC.put("requestId", "corr-123");
        assertEquals("corr-123", CorrelationIdUtils.getCurrentCorrelationId());
    }

    @Test
    void testGetCurrentCorrelationId_WhenNotSet() {
        assertNull(CorrelationIdUtils.getCurrentCorrelationId());
    }

    @Test
    void testHasCorrelationId_WhenSet() {
        MDC.put("requestId", "corr-123");
        assertTrue(CorrelationIdUtils.hasCorrelationId());
    }

    @Test
    void testHasCorrelationId_WhenNotSet() {
        assertFalse(CorrelationIdUtils.hasCorrelationId());
    }

    @Test
    void testGetOrDefault_WhenSet() {
        MDC.put("requestId", "corr-123");
        assertEquals("corr-123", CorrelationIdUtils.getOrDefault("fallback"));
    }

    @Test
    void testGetOrDefault_WhenNotSet() {
        assertEquals("fallback", CorrelationIdUtils.getOrDefault("fallback"));
    }
}
