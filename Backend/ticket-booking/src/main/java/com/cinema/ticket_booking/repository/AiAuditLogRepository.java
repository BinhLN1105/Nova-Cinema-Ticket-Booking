package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.AiAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AiAuditLogRepository extends JpaRepository<AiAuditLog, UUID> {
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
