package com.lodhi.notification_service.websocket;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WebSocketConfigTest {

    @Test
    void resolveAllowedOrigins_ExplicitList_IsTrimmedAndReturned() {
        String[] origins = WebSocketConfig.resolveAllowedOrigins(
                " http://localhost:5173 , https://authvolt.fun ");

        assertArrayEquals(
                new String[]{"http://localhost:5173", "https://authvolt.fun"}, origins);
    }

    @Test
    void resolveAllowedOrigins_Wildcard_IsRejected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> WebSocketConfig.resolveAllowedOrigins("*"));
        assertTrue(ex.getMessage().contains("*"));
    }

    @Test
    void resolveAllowedOrigins_WildcardAmongOrigins_IsRejected() {
        assertThrows(IllegalStateException.class,
                () -> WebSocketConfig.resolveAllowedOrigins("https://authvolt.fun,*"));
    }

    @Test
    void resolveAllowedOrigins_BlankOrNull_IsRejected() {
        assertThrows(IllegalStateException.class,
                () -> WebSocketConfig.resolveAllowedOrigins("   "));
        assertThrows(IllegalStateException.class,
                () -> WebSocketConfig.resolveAllowedOrigins(null));
    }

    @Test
    void resolveAllowedOrigins_BlankEntryAmongOrigins_IsRejected() {
        assertThrows(IllegalStateException.class,
                () -> WebSocketConfig.resolveAllowedOrigins("https://a.example,,https://b.example"));
    }
}
