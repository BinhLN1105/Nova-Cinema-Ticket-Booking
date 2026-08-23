package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.AiAuditLogResponse;
import com.cinema.ticket_booking.dto.response.CustomerSearchResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.enums.MembershipTier;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAuditLogServiceImplTest {

    @Mock
    private AiAuditLogRepository aiAuditLogRepository;

    @Mock
    private AiAuditLogAccessRepository aiAuditLogAccessRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerSearchLogRepository customerSearchLogRepository;

    @InjectMocks
    private AiAuditLogServiceImpl aiAuditLogService;

    private UUID userId;
    private UUID staffId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        staffId = UUID.randomUUID();
        adminId = UUID.randomUUID();
    }

    @Test
    @DisplayName("1. logInteractionAsync ghi log tương tác thành công")
    void testLogInteractionAsyncSuccess() {
        aiAuditLogService.logInteractionAsync(userId, "sess_123", "Tin nhắn user", "Trả lời bot", "SHOWTIME", false);
        verify(aiAuditLogRepository, times(1)).save(any(AiAuditLog.class));
    }

    @Test
    @DisplayName("2. searchCustomers từ khóa >= 3 ký tự trả về kết quả và ghi CustomerSearchLog")
    void testSearchCustomersSuccess() {
        User user = User.builder()
                .id(userId)
                .fullName("Nguyễn Văn A")
                .email("a@gmail.com")
                .phone("0912345678")
                .membershipTier(MembershipTier.GOLD)
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userRepository.searchCustomersByIdentifier(eq("0912345678"), any(Pageable.class))).thenReturn(page);

        PageResponse<CustomerSearchResponse> response = aiAuditLogService.searchCustomers(
                "0912345678", staffId, UserRole.STAFF, "127.0.0.1", PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Nguyễn Văn A", response.getContent().get(0).getFullName());
        verify(customerSearchLogRepository, times(1)).save(any(CustomerSearchLog.class));
    }

    @Test
    @DisplayName("3. searchCustomers từ khóa < 3 ký tự ném AppException 400")
    void testSearchCustomersShortQueryThrowsException() {
        assertThrows(AppException.class, () ->
                aiAuditLogService.searchCustomers("ab", staffId, UserRole.STAFF, "127.0.0.1", PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("4. getLogsBySession thành công cho STAFF có reason >= 10 ký tự")
    void testGetLogsBySessionStaffSuccess() {
        AiAuditLog logItem = AiAuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionId("sess_123")
                .userMessage("Hỏi giá vé")
                .aiResponse("Giá vé là 90k ạ")
                .intent("PRICING")
                .usedFallback(false)
                .createdAt(LocalDateTime.now())
                .build();

        Page<AiAuditLog> page = new PageImpl<>(List.of(logItem), PageRequest.of(0, 20), 1);
        when(aiAuditLogRepository.findBySessionIdOrderByCreatedAtAsc(eq("sess_123"), any(Pageable.class))).thenReturn(page);

        PageResponse<AiAuditLogResponse> response = aiAuditLogService.getLogsBySession(
                "sess_123", staffId, UserRole.STAFF, "Khách khiếu nại qua hotline", "TK-01", "127.0.0.1", PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(aiAuditLogAccessRepository, times(1)).save(any(AiAuditLogAccess.class));
    }

    @Test
    @DisplayName("5. getLogsBySession cho STAFF ném AppException khi reason < 10 ký tự")
    void testGetLogsBySessionStaffShortReasonThrowsException() {
        assertThrows(AppException.class, () ->
                aiAuditLogService.getLogsBySession("sess_123", staffId, UserRole.STAFF, "abc", null, "127.0.0.1", PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("6. getLogsByUser cho STAFF lọc 30 ngày gần nhất")
    void testGetLogsByUserStaffFilter30Days() {
        AiAuditLog logItem = AiAuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionId("sess_123")
                .userMessage("Hỏi phim")
                .aiResponse("Phim Mai")
                .intent("MOVIE_INFO")
                .usedFallback(false)
                .createdAt(LocalDateTime.now())
                .build();

        Page<AiAuditLog> page = new PageImpl<>(List.of(logItem), PageRequest.of(0, 20), 1);
        when(aiAuditLogRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(eq(userId), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<AiAuditLogResponse> response = aiAuditLogService.getLogsByUser(
                userId, staffId, UserRole.STAFF, "Khách yêu cầu tra cứu", "TK-02", "127.0.0.1", PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(aiAuditLogRepository, times(1)).findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(eq(userId), any(), any());
    }

    @Test
    @DisplayName("7. getLogsByUser cho ADMIN tra cứu toàn bộ lịch sử không giới hạn 30 ngày")
    void testGetLogsByUserAdminFullHistory() {
        AiAuditLog logItem = AiAuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionId("sess_123")
                .userMessage("Hỏi phim")
                .aiResponse("Phim Mai")
                .intent("MOVIE_INFO")
                .usedFallback(false)
                .createdAt(LocalDateTime.now().minusMonths(6))
                .build();

        Page<AiAuditLog> page = new PageImpl<>(List.of(logItem), PageRequest.of(0, 20), 1);
        when(aiAuditLogRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<AiAuditLogResponse> response = aiAuditLogService.getLogsByUser(
                userId, adminId, UserRole.ADMIN, null, null, "127.0.0.1", PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(aiAuditLogRepository, times(1)).findByUserIdOrderByCreatedAtDesc(eq(userId), any());
    }
}
