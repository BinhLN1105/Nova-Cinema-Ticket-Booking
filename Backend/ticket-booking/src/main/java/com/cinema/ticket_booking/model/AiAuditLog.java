package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.security.crypto.AesGcmAttributeConverter;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(name = "user_message", length = 4000)
    private String userMessage;

    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(name = "ai_response", length = 8000)
    private String aiResponse;

    @Column(name = "intent")
    private String intent;

    @Column(name = "used_fallback")
    private boolean usedFallback;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
