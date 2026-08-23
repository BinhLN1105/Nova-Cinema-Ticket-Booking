package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.AiAuditLogResponse;
import com.cinema.ticket_booking.dto.response.CustomerSearchResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.exception.AppException;
import com.cinema.ticket_booking.model.AiAuditLog;
import com.cinema.ticket_booking.model.AiAuditLogAccess;
import com.cinema.ticket_booking.model.CustomerSearchLog;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.AiAuditLogAccessRepository;
import com.cinema.ticket_booking.repository.AiAuditLogRepository;
import com.cinema.ticket_booking.repository.CustomerSearchLogRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.AiAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAuditLogServiceImpl implements AiAuditLogService {

    private final AiAuditLogRepository aiAuditLogRepository;
    private final AiAuditLogAccessRepository aiAuditLogAccessRepository;
    private final UserRepository userRepository;
    private final CustomerSearchLogRepository customerSearchLogRepository;

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

    @Override
    @Transactional
    public PageResponse<CustomerSearchResponse> searchCustomers(String query, UUID accessorUserId,
            UserRole accessorRole, String ipAddress, Pageable pageable) {
        if (query == null || query.trim().length() < 3) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Từ khóa tìm kiếm khách hàng phải có ít nhất 3 ký tự để chống rò rỉ dữ liệu.");
        }

        String cleanQuery = query.trim();
        Page<User> usersPage = userRepository.searchCustomersByIdentifier(cleanQuery, pageable);

        // Ghi vết hành vi tìm kiếm (Customer Search Log)
        try {
            CustomerSearchLog searchLog = CustomerSearchLog.builder()
                    .accessorUserId(accessorUserId)
                    .accessorRole(accessorRole)
                    .searchQuery(cleanQuery)
                    .resultCount((int) usersPage.getTotalElements())
                    .ipAddress(ipAddress)
                    .build();
            customerSearchLogRepository.save(searchLog);
        } catch (Exception e) {
            log.error("[Customer Search Log Error] {}", e.getMessage(), e);
        }

        List<CustomerSearchResponse> dtoList = usersPage.getContent().stream()
                .map(this::mapToCustomerSearchResponse)
                .toList();

        return PageResponse.of(usersPage, dtoList);
    }

    @Override
    @Transactional
    public PageResponse<AiAuditLogResponse> getLogsBySession(String sessionId, UUID accessorUserId,
            UserRole accessorRole, String reason, String ticketId, String ipAddress, Pageable pageable) {
        // Validate lý do tra cứu đối với nhân viên CSKH (STAFF)
        String effectiveReason = validateAndFormatReason(accessorRole, reason, ticketId);

        log.info("[Audit Log Access] User {} (Role: {}) accessed logs for sessionId: {} with reason: {}",
                accessorUserId, accessorRole, sessionId, effectiveReason);

        // Ghi vết hành vi truy cập (Audit the Auditor)
        recordAccessHistory(accessorUserId, accessorRole, sessionId, null, ipAddress, effectiveReason);

        Page<AiAuditLog> logPage = aiAuditLogRepository.findBySessionIdOrderByCreatedAtAsc(sessionId, pageable);
        List<AiAuditLogResponse> dtoList = logPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(logPage, dtoList);
    }

    @Override
    @Transactional
    public PageResponse<AiAuditLogResponse> getLogsByUser(UUID targetUserId, UUID accessorUserId,
            UserRole accessorRole, String reason, String ticketId, String ipAddress, Pageable pageable) {
        // Validate lý do tra cứu đối với nhân viên CSKH (STAFF)
        String effectiveReason = validateAndFormatReason(accessorRole, reason, ticketId);

        log.info("[Audit Log Access] User {} (Role: {}) accessed logs for targetUserId: {} with reason: {}",
                accessorUserId, accessorRole, targetUserId, effectiveReason);

        // Ghi vết hành vi truy cập (Audit the Auditor)
        recordAccessHistory(accessorUserId, accessorRole, null, targetUserId, ipAddress, effectiveReason);

        Page<AiAuditLog> logPage;
        if (accessorRole == UserRole.STAFF) {
            // Giới hạn phạm vi thời gian: Nhân viên CSKH chỉ được tra cứu tối đa 30 ngày gần nhất
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            logPage = aiAuditLogRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(targetUserId, cutoff,
                    pageable);
        } else {
            // ADMIN: Được phép tra cứu toàn bộ lịch sử
            logPage = aiAuditLogRepository.findByUserIdOrderByCreatedAtDesc(targetUserId, pageable);
        }

        List<AiAuditLogResponse> dtoList = logPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(logPage, dtoList);
    }

    private String validateAndFormatReason(UserRole role, String reason, String ticketId) {
        if (role == UserRole.STAFF) {
            if (reason == null || reason.trim().length() < 10) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "Nhân viên CSKH bắt buộc phải cung cấp lý do tra cứu rõ ràng (ít nhất 10 ký tự).");
            }
        }
        String cleanReason = (reason != null && !reason.isBlank()) ? reason.trim() : "System Audit / Administration";
        if (ticketId != null && !ticketId.isBlank()) {
            cleanReason = "[Ticket: " + ticketId.trim() + "] " + cleanReason;
        }
        return cleanReason;
    }

    private void recordAccessHistory(UUID accessorUserId, UserRole accessorRole, String targetSessionId,
            UUID targetUserId, String ipAddress, String reason) {
        try {
            AiAuditLogAccess access = AiAuditLogAccess.builder()
                    .accessorUserId(accessorUserId)
                    .accessorRole(accessorRole)
                    .targetSessionId(targetSessionId)
                    .targetUserId(targetUserId)
                    .ipAddress(ipAddress)
                    .reason(reason)
                    .build();
            aiAuditLogAccessRepository.save(access);
        } catch (Exception e) {
            log.error("[Audit Access Error] Failed to record access log: {}", e.getMessage(), e);
        }
    }

    private CustomerSearchResponse mapToCustomerSearchResponse(User user) {
        return CustomerSearchResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .membershipTier(user.getMembershipTier())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AiAuditLogResponse mapToResponse(AiAuditLog logItem) {
        return AiAuditLogResponse.builder()
                .id(logItem.getId())
                .userId(logItem.getUserId())
                .sessionId(logItem.getSessionId())
                .userMessage(logItem.getUserMessage()) // JPA tự động giải mã từ ciphertext -> plaintext
                .aiResponse(logItem.getAiResponse())   // JPA tự động giải mã từ ciphertext -> plaintext
                .intent(logItem.getIntent())
                .usedFallback(logItem.isUsedFallback())
                .createdAt(logItem.getCreatedAt())
                .build();
    }
}

