package com.lodhi.auth.respositories;

import com.lodhi.auth.audit.AuditLog;
import com.lodhi.auth.audit.AuditEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    List<AuditLog> findByUserIdOrderByTimestampDesc(Long userId);
    
    List<AuditLog> findByEventTypeAndTimestampAfter(AuditEventType eventType, Instant after);
    
    long countByUserIdAndEventTypeAndSuccessAndTimestampAfter(
        Long userId, 
        AuditEventType eventType, 
        boolean success, 
        Instant after
    );
}
