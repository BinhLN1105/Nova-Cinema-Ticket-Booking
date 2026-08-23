package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.AiAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AiAuditLogRepository extends JpaRepository<AiAuditLog, UUID> {
    void deleteByCreatedAtBefore(LocalDateTime dateTime);

    Page<AiAuditLog> findBySessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);

    List<AiAuditLog> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    Page<AiAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AiAuditLog> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID userId, LocalDateTime after, Pageable pageable);
}
