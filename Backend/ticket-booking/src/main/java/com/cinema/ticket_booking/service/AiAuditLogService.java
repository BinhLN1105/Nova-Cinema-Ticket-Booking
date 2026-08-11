package com.cinema.ticket_booking.service;

import java.util.UUID;

public interface AiAuditLogService {
    void logInteractionAsync(UUID userId, String sessionId, String userMessage, String reply, String intent,
            boolean usedFallback);
}
