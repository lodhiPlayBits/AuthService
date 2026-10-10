package com.lodhi.auth.audit;

import java.time.Instant;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.lodhi.auth.respositories.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditService self;
    
    public AuditService(AuditLogRepository auditLogRepository, @Lazy AuditService self) {
        this.auditLogRepository = auditLogRepository;
        this.self = self;
    }

    @Async
    public void logEventAsync(
            Long userId,
            String username,
            AuditEventType eventType,
            boolean success,
            String details,
            String ipAddress,
            String userAgent
    ) {
        try {
            String dbUsername = username;
            if (!success && eventType == AuditEventType.LOGIN_FAILURE) {
                dbUsername = com.lodhi.auth.utils.LoggingUtils.maskIdentifier(username);
            }

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(dbUsername)
                    .eventType(eventType)
                    .success(success)
                    .details(details)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .timestamp(Instant.now())
                    .build();

            auditLogRepository.save(auditLog);
            
            log.info("Audit logged: user={}, event={}, success={}, ip={}", 
                com.lodhi.auth.utils.LoggingUtils.maskIdentifier(username), eventType, success, auditLog.getIpAddress());
                
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    public void logEvent(
            Long userId,
            String username,
            AuditEventType eventType,
            boolean success,
            String details,
            HttpServletRequest request
    ) {
        if (request == null) {
            self.logEventAsync(userId, username, eventType, success, details, "unknown", "unknown");
            return;
        }
        String ipAddress = com.lodhi.auth.utils.LoggingUtils.extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        self.logEventAsync(userId, username, eventType, success, details, ipAddress, userAgent);
    }

    public void logLoginSuccess(Long userId, String username, HttpServletRequest request) {
        logEvent(userId, username, AuditEventType.LOGIN_SUCCESS, true, 
                "User logged in successfully", request);
    }

    public void logLoginFailure(String identifier, String reason, HttpServletRequest request) {
        logEvent(null, identifier, AuditEventType.LOGIN_FAILURE, false, 
                "Login failed: " + reason, request);
    }

    public void logTokenRefresh(Long userId, String username, HttpServletRequest request) {
        logEvent(userId, username, AuditEventType.TOKEN_REFRESH, true, 
                "Token refreshed", request);
    }
    
    public void logTokenRefreshFailure(String identifier, String reason, HttpServletRequest request) {
        logEvent(null, identifier, AuditEventType.TOKEN_REFRESH, false, 
                "Token refresh failed: " + reason, request);
    }

    public void logTokenReuse(Long userId, String identifier, HttpServletRequest request) {
        logEvent(userId, identifier, AuditEventType.TOKEN_REUSE, false, 
                "Token reuse detected", request);
    }

    public void logLogout(Long userId, String username, HttpServletRequest request) {
        logEvent(userId, username, AuditEventType.LOGOUT, true, 
                "User logged out", request);
    }
    
    /**
     * Simplified audit logging for service-layer operations where HttpServletRequest is not available.
     * Used for system/background operations.
     */
    @Async
    public void logServiceEvent(
            Long userId,
            String username,
            AuditEventType eventType,
            boolean success,
            String details
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .eventType(eventType)
                    .success(success)
                    .details(details)
                    .ipAddress("system")  // No request context
                    .userAgent("system")  // No request context
                    .timestamp(Instant.now())
                    .build();

            auditLogRepository.save(auditLog);
            
            log.info("Audit logged (service): user={}, event={}, success={}", 
                com.lodhi.auth.utils.LoggingUtils.maskIdentifier(username), eventType, success);
                
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }
}
