package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.BookingRequest;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.exception.GlobalExceptionHandler;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.enums.MembershipTier;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.BookingService;
import com.cinema.ticket_booking.service.SystemConfigService;
import com.cinema.ticket_booking.service.impl.ScanLogServiceImpl;
import com.cinema.ticket_booking.controller.ai.AiAgentController.DraftBookingCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;
    @Mock
    private ScanLogServiceImpl scanLogService;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BookingController bookingController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .membershipTier(MembershipTier.BRONZE)
                .rankUsageThisMonth(0)
                .build();

        bookingController = new BookingController(
                bookingService,
                scanLogService,
                systemConfigService,
                redisTemplate,
                objectMapper,
                userRepository);

        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(User.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return mockUser;
                    }
                })
                .build();
    }

    @Test
    void testConfirmDraftBooking_Success() throws Exception {
        String draftId = UUID.randomUUID().toString();
        DraftBookingCache draft = new DraftBookingCache();
        draft.setUserId(mockUser.getId());
        draft.setShowtimeId(UUID.randomUUID().toString());
        draft.setShowtimeSeatIds(List.of("seat1"));

        String draftJson = objectMapper.writeValueAsString(draft);

        when(userRepository.findById(mockUser.getId())).thenReturn(Optional.of(mockUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai_draft_booking:" + draftId)).thenReturn(draftJson);
        when(valueOperations.setIfAbsent(eq("lock:booking_confirm:" + draftId), anyString(), any(Duration.class)))
                .thenReturn(true);

        BookingResponse mockResponse = BookingResponse.builder()
                .id(UUID.randomUUID().toString())
                .totalAmount(BigDecimal.valueOf(120000))
                .build();
        when(bookingService.createBooking(eq(mockUser.getId()), any(BookingRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/bookings/draft-confirm")
                .param("draftId", draftId)
                .param("paymentMethod", "VNPAY"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xác nhận đặt vé thành công từ đơn nháp"));

        verify(redisTemplate).delete("ai_draft_booking:" + draftId);
        verify(redisTemplate).delete("lock:booking_confirm:" + draftId);
        verify(userRepository).save(mockUser);
    }

    @Test
    void testConfirmDraftBooking_NotFoundOrExpired() throws Exception {
        String draftId = UUID.randomUUID().toString();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai_draft_booking:" + draftId)).thenReturn(null);

        mockMvc.perform(post("/api/v1/bookings/draft-confirm")
                .param("draftId", draftId)
                .param("paymentMethod", "VNPAY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Đơn nháp không tồn tại hoặc đã hết hạn (tối đa 10 phút)."));
    }

    @Test
    void testConfirmDraftBooking_CrossAuthorizationError() throws Exception {
        String draftId = UUID.randomUUID().toString();
        DraftBookingCache draft = new DraftBookingCache();
        draft.setUserId(UUID.randomUUID()); // Different user
        draft.setShowtimeId(UUID.randomUUID().toString());
        draft.setShowtimeSeatIds(List.of("seat1"));

        String draftJson = objectMapper.writeValueAsString(draft);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai_draft_booking:" + draftId)).thenReturn(draftJson);

        mockMvc.perform(post("/api/v1/bookings/draft-confirm")
                .param("draftId", draftId)
                .param("paymentMethod", "VNPAY"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện xác nhận giao dịch này."));
    }

    @Test
    void testConfirmDraftBooking_ConcurrencyConflict() throws Exception {
        String draftId = UUID.randomUUID().toString();
        DraftBookingCache draft = new DraftBookingCache();
        draft.setUserId(mockUser.getId());
        draft.setShowtimeId(UUID.randomUUID().toString());
        draft.setShowtimeSeatIds(List.of("seat1"));

        String draftJson = objectMapper.writeValueAsString(draft);

        when(userRepository.findById(mockUser.getId())).thenReturn(Optional.of(mockUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai_draft_booking:" + draftId)).thenReturn(draftJson);
        // Simulate lock acquisition fails (returns false)
        when(valueOperations.setIfAbsent(eq("lock:booking_confirm:" + draftId), anyString(), any(Duration.class)))
                .thenReturn(false);

        mockMvc.perform(post("/api/v1/bookings/draft-confirm")
                .param("draftId", draftId)
                .param("paymentMethod", "VNPAY"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message").value("Giao dịch đang được xử lý, vui lòng không gửi yêu cầu liên tục."));
    }

    @Test
    void testConfirmDraftBooking_RankLimitExceeded() throws Exception {
        String draftId = UUID.randomUUID().toString();
        DraftBookingCache draft = new DraftBookingCache();
        draft.setUserId(mockUser.getId());
        draft.setShowtimeId(UUID.randomUUID().toString());
        draft.setShowtimeSeatIds(List.of("seat1"));

        String draftJson = objectMapper.writeValueAsString(draft);

        // Giả lập Bronze đạt tối đa 2 lần/tháng
        mockUser.setRankUsageThisMonth(2);

        when(userRepository.findById(mockUser.getId())).thenReturn(Optional.of(mockUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai_draft_booking:" + draftId)).thenReturn(draftJson);

        mockMvc.perform(post("/api/v1/bookings/draft-confirm")
                .param("draftId", draftId)
                .param("paymentMethod", "VNPAY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        containsString("Hạng thành viên BRONZE của bạn chỉ được đặt tối đa 2 vé qua AI mỗi tháng")));
    }
}
