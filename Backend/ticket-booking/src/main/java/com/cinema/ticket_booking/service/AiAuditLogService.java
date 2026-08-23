package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.AiAuditLogResponse;
import com.cinema.ticket_booking.dto.response.CustomerSearchResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.enums.UserRole;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AiAuditLogService {
    void logInteractionAsync(UUID userId, String sessionId, String userMessage, String reply, String intent,
            boolean usedFallback);

    PageResponse<CustomerSearchResponse> searchCustomers(String query, UUID accessorUserId, UserRole accessorRole,
            String ipAddress, Pageable pageable);

    PageResponse<AiAuditLogResponse> getLogsBySession(String sessionId, UUID accessorUserId, UserRole accessorRole,
            String reason, String ticketId, String ipAddress, Pageable pageable);

    PageResponse<AiAuditLogResponse> getLogsByUser(UUID targetUserId, UUID accessorUserId, UserRole accessorRole,
            String reason, String ticketId, String ipAddress, Pageable pageable);
}
