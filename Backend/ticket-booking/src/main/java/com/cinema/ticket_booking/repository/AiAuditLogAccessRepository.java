package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.AiAuditLogAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiAuditLogAccessRepository extends JpaRepository<AiAuditLogAccess, UUID> {
    Page<AiAuditLogAccess> findByAccessorUserIdOrderByAccessedAtDesc(UUID accessorUserId, Pageable pageable);
    Page<AiAuditLogAccess> findByTargetSessionIdOrderByAccessedAtDesc(String targetSessionId, Pageable pageable);
}
