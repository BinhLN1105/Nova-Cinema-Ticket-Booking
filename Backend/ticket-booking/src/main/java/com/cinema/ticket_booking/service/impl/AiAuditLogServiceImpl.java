package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.model.AiAuditLog;
import com.cinema.ticket_booking.repository.AiAuditLogRepository;
import com.cinema.ticket_booking.service.AiAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAuditLogServiceImpl implements AiAuditLogService {

    private final AiAuditLogRepository aiAuditLogRepository;

    @Override
    @Async
    public void logInteractionAsync(UUID userId, String sessionId, String userMessage, String reply, String intent,
            boolean usedFallback) {
        try {
            log.info("[Async Audit Log] Recording interaction for user: {}", userId);
            AiAuditLog auditLog = AiAuditLog.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .userMessage(userMessage)
                    .aiResponse(reply)
                    .intent(intent)
                    .usedFallback(usedFallback)
                    .build();
            aiAuditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("[Async Audit Log] Failed to save audit log: {}", e.getMessage(), e);
        }
    }
}
