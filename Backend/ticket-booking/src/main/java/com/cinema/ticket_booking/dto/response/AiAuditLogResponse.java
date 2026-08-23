package com.cinema.ticket_booking.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAuditLogResponse {
    private UUID id;
    private UUID userId;
    private String sessionId;
    private String userMessage;
    private String aiResponse;
    private String intent;
    private boolean usedFallback;
    private LocalDateTime createdAt;
}
