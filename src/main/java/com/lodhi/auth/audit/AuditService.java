package com.lodhi.auth.audit;

import com.lodhi.auth.respositories.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void logEvent(
            Long userId,
            String username,
            AuditEventType eventType,
            boolean success,
            String details,
            HttpServletRequest request
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .eventType(eventType)
                    .success(success)
                    .details(details)
                    .ipAddress(extractIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .timestamp(Instant.now())
                    .build();

            auditLogRepository.save(auditLog);
            
            log.info("Audit logged: user={}, event={}, success={}, ip={}", 
                username, eventType, success, auditLog.getIpAddress());
                
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    @Async
    public void logLoginSuccess(Long userId, String username, HttpServletRequest request) {
        logEvent(userId, username, AuditEventType.LOGIN_SUCCESS, true, 
                "User logged in successfully", request);
    }

    @Async
    public void logLoginFailure(String identifier, String reason, HttpServletRequest request) {
        logEvent(null, identifier, AuditEventType.LOGIN_FAILURE, false, 
                "Login failed: " + reason, request);
    }

    @Async
    public void logTokenRefresh(Long userId, String username, HttpServletRequest request) {
        logEvent(userId, username, AuditEventType.TOKEN_REFRESH, true, 
                "Token refreshed", request);
    }

    @Async
    public void logLogout(Long userId, String username, HttpServletRequest request) {
        logEvent(userId, username, AuditEventType.LOGOUT, true, 
                "User logged out", request);
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
