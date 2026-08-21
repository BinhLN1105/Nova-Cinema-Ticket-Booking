package com.cinema.ticket_booking.controller.ai;

import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeResponse;
import com.cinema.ticket_booking.dto.response.WeatherShowtimeResponse;
import com.cinema.ticket_booking.enums.MembershipTier;
import com.cinema.ticket_booking.enums.NotificationType;
import com.cinema.ticket_booking.enums.ReminderType;
import com.cinema.ticket_booking.model.Notification;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.NotificationRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.BookingService;
import com.cinema.ticket_booking.service.ShowtimeService;
import com.cinema.ticket_booking.service.WeatherIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AiAgentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ShowtimeService showtimeService;
    @Mock
    private WeatherIntegrationService weatherIntegrationService;

    @InjectMocks
    private AiAgentController aiAgentController;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    private final String internalApiKey = "test-internal-api-key";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiAgentController).build();
        ReflectionTestUtils.setField(aiAgentController, "internalApiKey", internalApiKey);
    }

    @Test
    void testCreateDraftBooking_Success() throws Exception {
        String sessionId = "sess-12345";
        UUID userId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai_session_user:" + sessionId)).thenReturn(userId.toString());

        AiAgentController.AiDraftBookingRequest req = new AiAgentController.AiDraftBookingRequest();
        req.setShowtimeId("st-123");
        req.setSeatIds(List.of("seat-1", "seat-2"));

        BookingResponse mockQuote = BookingResponse.builder()
                .totalAmount(BigDecimal.valueOf(250000))
                .movieTitle("Kung Fu Panda 4")
                .startTime(LocalDateTime.now())
                .cinemaName("Nova Cinema Center")
                .build();

        when(bookingService.calculateQuote(eq(userId), any())).thenReturn(mockQuote);

        mockMvc.perform(post("/internal/api/ai/booking/draft")
                .header("X-Internal-Key", internalApiKey)
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.draftId").exists())
                .andExpect(jsonPath("$.data.totalAmount").value(250000))
                .andExpect(jsonPath("$.data.movieTitle").value("Kung Fu Panda 4"));
    }

    @Test
    void testCreateDraftReminder_Success() throws Exception {
        String sessionId = "sess-12345";
        UUID userId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai_session_user:" + sessionId)).thenReturn(userId.toString());
        when(valueOperations.increment(anyString())).thenReturn(1L);

        User mockUser = new User();
        mockUser.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        ShowtimeResponse mockShowtime = ShowtimeResponse.builder()
                .id(UUID.randomUUID().toString())
                .movieTitle("Dune 2")
                .cinemaName("IMAX Nova Room")
                .startTime(LocalDateTime.now())
                .build();
        when(showtimeService.getById(any(UUID.class))).thenReturn(mockShowtime);

        AiAgentController.AiDraftReminderRequest req = new AiAgentController.AiDraftReminderRequest();
        req.setShowtimeId(UUID.randomUUID().toString());

        mockMvc.perform(post("/internal/api/ai/reminder/draft")
                .header("X-Internal-Key", internalApiKey)
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã cài đặt nhắc nhở lịch xem phim thành công"));

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testGetWeatherForShowtime_Success() throws Exception {
        UUID showtimeId = UUID.randomUUID();
        WeatherShowtimeResponse mockResp = WeatherShowtimeResponse.builder()
                .condition("Trời quang")
                .temperature(30.0)
                .isBadWeather(false)
                .build();

        when(weatherIntegrationService.getWeatherForShowtime(showtimeId)).thenReturn(mockResp);

        mockMvc.perform(get("/internal/api/ai/weather/showtime/" + showtimeId)
                .header("X-Internal-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.condition").value("Trời quang"))
                .andExpect(jsonPath("$.data.temperature").value(30.0));
    }

    @Test
    void testGetWeatherForCinema_Success() throws Exception {
        UUID cinemaId = UUID.randomUUID();
        WeatherShowtimeResponse mockResp = WeatherShowtimeResponse.builder()
                .condition("Nắng nhẹ")
                .temperature(32.0)
                .isBadWeather(false)
                .build();

        when(weatherIntegrationService.getWeatherForCinema(cinemaId)).thenReturn(mockResp);

        mockMvc.perform(get("/internal/api/ai/weather/cinema/" + cinemaId)
                .header("X-Internal-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.condition").value("Nắng nhẹ"))
                .andExpect(jsonPath("$.data.temperature").value(32.0));
    }

    @Test
    void testGetReminderList_Success() throws Exception {
        String sessionId = "sess-12345";
        UUID userId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai_session_user:" + sessionId)).thenReturn(userId.toString());

        Notification notif = Notification.builder()
                .id(UUID.randomUUID())
                .title("Nhắc lịch")
                .body("Chiếu lúc 20:00")
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByUserIdAndTypeOrderBySentAtDesc(eq(userId), eq(NotificationType.REMINDER)))
                .thenReturn(List.of(notif));

        mockMvc.perform(get("/internal/api/ai/reminder/list")
                .header("X-Internal-Key", internalApiKey)
                .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Nhắc lịch"));
    }

    @Test
    void testDeleteReminder_Success() throws Exception {
        String sessionId = "sess-12345";
        UUID userId = UUID.randomUUID();
        UUID reminderId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai_session_user:" + sessionId)).thenReturn(userId.toString());

        User user = new User();
        user.setId(userId);

        Notification notif = Notification.builder()
                .id(reminderId)
                .user(user)
                .build();

        when(notificationRepository.findById(reminderId)).thenReturn(Optional.of(notif));

        mockMvc.perform(delete("/internal/api/ai/reminder/" + reminderId)
                .header("X-Internal-Key", internalApiKey)
                .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xóa nhắc lịch thành công"));

        verify(notificationRepository, times(1)).delete(notif);
    }

    @Test
    void testDeleteAllReminders_Success() throws Exception {
        String sessionId = "sess-12345";
        UUID userId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai_session_user:" + sessionId)).thenReturn(userId.toString());

        mockMvc.perform(delete("/internal/api/ai/reminder/all")
                .header("X-Internal-Key", internalApiKey)
                .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xóa toàn bộ nhắc lịch thành công"));

        verify(notificationRepository, times(1)).deleteByUserIdAndType(userId, NotificationType.REMINDER);
    }

    @Test
    void testGetUserTickets_Success() throws Exception {
        String sessionId = "sess-12345";
        UUID userId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai_session_user:" + sessionId)).thenReturn(userId.toString());

        User user = new User();
        user.setId(userId);
        user.setMembershipTier(MembershipTier.GOLD);
        user.setRewardPoints(1500L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookingService.getMyBookings(eq(userId), any(Pageable.class)))
                .thenReturn(com.cinema.ticket_booking.dto.response.PageResponse.<BookingResponse.Summary>builder()
                        .content(List.of())
                        .build());

        mockMvc.perform(get("/internal/api/ai/user/tickets")
                .header("X-Internal-Key", internalApiKey)
                .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rank").value("Vàng"))
                .andExpect(jsonPath("$.data.cinePoints").value(1500));
    }
}
