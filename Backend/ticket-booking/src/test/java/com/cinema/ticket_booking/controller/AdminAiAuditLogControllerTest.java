package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.AiAuditLogResponse;
import com.cinema.ticket_booking.dto.response.CustomerSearchResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.enums.MembershipTier;
import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.exception.AppException;
import com.cinema.ticket_booking.exception.GlobalExceptionHandler;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.AiAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminAiAuditLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AiAuditLogService aiAuditLogService;

    @InjectMocks
    private AdminAiAuditLogController controller;

    private User mockStaffUser;

    @BeforeEach
    void setUp() {
        mockStaffUser = User.builder()
                .id(UUID.randomUUID())
                .email("cskh@novaticket.vn")
                .fullName("Nhân viên CSKH")
                .role(UserRole.STAFF)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return mockStaffUser;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("1. Bước 1: CSKH tìm kiếm khách hàng theo SĐT/Email/Tên hợp lệ trả về thông tin định danh 200 OK")
    void testSearchCustomersValidQuery() throws Exception {
        CustomerSearchResponse customer = CustomerSearchResponse.builder()
                .id(UUID.randomUUID())
                .fullName("Nguyễn Văn A")
                .email("nguyenvana@gmail.com")
                .phone("0912345678")
                .membershipTier(MembershipTier.GOLD)
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        Page<CustomerSearchResponse> page = new PageImpl<>(List.of(customer), PageRequest.of(0, 20), 1);
        PageResponse<CustomerSearchResponse> pageResponse = PageResponse.of(page);

        when(aiAuditLogService.searchCustomers(eq("0912345678"), eq(mockStaffUser.getId()), eq(UserRole.STAFF), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/ai-audit-logs/customers/search")
                        .param("query", "0912345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.content[0].phone").value("0912345678"));
    }

    @Test
    @DisplayName("2. Bước 1: CSKH tìm kiếm với từ khóa < 3 ký tự bị chặn 400 Bad Request chống rò rỉ dữ liệu")
    void testSearchCustomersTooShortQueryFails() throws Exception {
        when(aiAuditLogService.searchCustomers(eq("ab"), eq(mockStaffUser.getId()), eq(UserRole.STAFF), any(), any(Pageable.class)))
                .thenThrow(new AppException(HttpStatus.BAD_REQUEST, "Từ khóa tìm kiếm khách hàng phải có ít nhất 3 ký tự để chống rò rỉ dữ liệu."));

        mockMvc.perform(get("/api/v1/admin/ai-audit-logs/customers/search")
                        .param("query", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("3. Bước 2: CSKH tra cứu chi tiết có lý do hợp lệ (>= 10 ký tự) trả về dữ liệu giải mã thành công")
    void testGetLogsBySessionWithValidReason() throws Exception {
        String sessionId = "sess_booking_trouble_123";
        String validReason = "Khách hàng khiếu nại qua hotline về sự cố thanh toán";
        AiAuditLogResponse item = AiAuditLogResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .sessionId(sessionId)
                .userMessage("Tại sao tôi không nhận được mã vé nháp?")
                .aiResponse("Dạ, em đã kiểm tra và thấy mã vé nháp của anh/chị là BK-999 ạ.")
                .intent("BOOKING_ISSUE")
                .usedFallback(false)
                .createdAt(LocalDateTime.now())
                .build();

        Page<AiAuditLogResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);
        PageResponse<AiAuditLogResponse> pageResponse = PageResponse.of(page);

        when(aiAuditLogService.getLogsBySession(eq(sessionId), eq(mockStaffUser.getId()), eq(UserRole.STAFF), eq(validReason), eq("TK-1234"), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/ai-audit-logs/session/{sessionId}", sessionId)
                        .param("reason", validReason)
                        .param("ticketId", "TK-1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.content[0].userMessage").value("Tại sao tôi không nhận được mã vé nháp?"))
                .andExpect(jsonPath("$.data.content[0].aiResponse").value("Dạ, em đã kiểm tra và thấy mã vé nháp của anh/chị là BK-999 ạ."));
    }

    @Test
    @DisplayName("4. Bước 2: CSKH tra cứu thiếu lý do hoặc lý do < 10 ký tự bị chặn 400 Bad Request")
    void testStaffWithoutReasonOrTooShortFails() throws Exception {
        String sessionId = "sess_booking_trouble_123";
        when(aiAuditLogService.getLogsBySession(eq(sessionId), eq(mockStaffUser.getId()), eq(UserRole.STAFF), eq("abc"), any(), any(), any(Pageable.class)))
                .thenThrow(new AppException(HttpStatus.BAD_REQUEST, "Nhân viên CSKH bắt buộc phải cung cấp lý do tra cứu rõ ràng (ít nhất 10 ký tự)."));

        mockMvc.perform(get("/api/v1/admin/ai-audit-logs/session/{sessionId}", sessionId)
                        .param("reason", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("5. Bước 2: CSKH tra cứu theo targetUserId phân trang thành công")
    void testGetLogsByUser() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        String validReason = "Khách yêu cầu tra cứu lịch sử tương tác AI tháng này";
        AiAuditLogResponse item = AiAuditLogResponse.builder()
                .id(UUID.randomUUID())
                .userId(targetUserId)
                .sessionId("sess_abc")
                .userMessage("Hỏi lịch chiếu phim Mai")
                .aiResponse("Lịch chiếu phim Mai lúc 20:00")
                .intent("SHOWTIME")
                .usedFallback(false)
                .createdAt(LocalDateTime.now())
                .build();

        Page<AiAuditLogResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);
        PageResponse<AiAuditLogResponse> pageResponse = PageResponse.of(page);

        when(aiAuditLogService.getLogsByUser(eq(targetUserId), eq(mockStaffUser.getId()), eq(UserRole.STAFF), eq(validReason), any(), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/ai-audit-logs/user/{userId}", targetUserId)
                        .param("reason", validReason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].userId").value(targetUserId.toString()))
                .andExpect(jsonPath("$.data.content[0].userMessage").value("Hỏi lịch chiếu phim Mai"));
    }
}
