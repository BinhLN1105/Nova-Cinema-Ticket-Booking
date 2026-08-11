package com.cinema.ticket_booking.controller.ai;

import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeResponse;
import com.cinema.ticket_booking.model.Notification;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.NotificationRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.service.BookingService;
import com.cinema.ticket_booking.service.ShowtimeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
