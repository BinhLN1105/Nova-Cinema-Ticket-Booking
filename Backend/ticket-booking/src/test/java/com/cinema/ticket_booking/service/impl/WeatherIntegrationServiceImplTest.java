package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.WeatherShowtimeResponse;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.model.CinemaWeatherCache;
import com.cinema.ticket_booking.model.Screen;
import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.repository.CinemaRepository;
import com.cinema.ticket_booking.repository.CinemaWeatherCacheRepository;
import com.cinema.ticket_booking.repository.ShowtimeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherIntegrationServiceImplTest {

    @Mock
    private CinemaWeatherCacheRepository weatherCacheRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private CinemaRepository cinemaRepository;
    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WeatherIntegrationServiceImpl weatherService;

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private UUID cinemaId;
    private UUID showtimeId;
    private Cinema cinema;
    private Showtime showtime;
    private CinemaWeatherCache cache;

    @BeforeEach
    void setUp() {
        cinemaId = UUID.randomUUID();
        showtimeId = UUID.randomUUID();

        cinema = Cinema.builder()
                .id(cinemaId)
                .name("Nova Cinema Center")
                .build();

        Screen screen = Screen.builder()
                .id(UUID.randomUUID())
                .name("Screen 1")
                .cinema(cinema)
                .build();

        showtime = Showtime.builder()
                .id(showtimeId)
                .screen(screen)
                .startTime(LocalDateTime.now(ZONE_VN).plusHours(2))
                .build();

        cache = CinemaWeatherCache.builder()
                .cinemaId(cinemaId)
                .latitude(10.8231)
                .longitude(106.6297)
                .lastWeatherData("{\"forecast\":{\"forecastday\":[{\"date\":\"" + LocalDateTime.now(ZONE_VN).toLocalDate() + "\",\"hour\":[{\"time\":\"" + LocalDateTime.now(ZONE_VN).toLocalDate() + " " + String.format("%02d:00", LocalDateTime.now(ZONE_VN).plusHours(2).getHour()) + "\",\"temp_c\":28.5,\"condition\":{\"text\":\"Trời quang mây tạnh\"}}]}]}}")
                .lastFetchedAt(LocalDateTime.now(ZONE_VN))
                .build();

        ReflectionTestUtils.setField(weatherService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(weatherService, "apiKey", "real_test_api_key");
    }

    @Test
    void testGetWeatherForCinema_CinemaNotFound() {
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.empty());

        WeatherShowtimeResponse response = weatherService.getWeatherForCinema(cinemaId);

        assertNotNull(response);
        assertTrue(response.isOutOfForecastRange());
        assertEquals("Không tìm thấy thông tin rạp chiếu.", response.getWarningMessage());
    }

    @Test
    void testGetWeatherForCinema_NoCoordinates() {
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(weatherCacheRepository.findById(cinemaId)).thenReturn(Optional.empty());

        WeatherShowtimeResponse response = weatherService.getWeatherForCinema(cinemaId);

        assertNotNull(response);
        assertTrue(response.isOutOfForecastRange());
        assertEquals("Rạp hiện chưa được cấu hình tọa độ thời tiết.", response.getWarningMessage());
    }

    @Test
    void testGetWeatherForCinema_MockMode() {
        ReflectionTestUtils.setField(weatherService, "apiKey", "mock_key");
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(weatherCacheRepository.findById(cinemaId)).thenReturn(Optional.of(cache));

        WeatherShowtimeResponse response = weatherService.getWeatherForCinema(cinemaId);

        assertNotNull(response);
        assertNotNull(response.getCondition());
        assertNotNull(response.getTemperature());
        assertFalse(response.isOutOfForecastRange());
    }

    @Test
    void testGetWeatherForShowtime_ShowtimeNotFound() {
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.empty());

        WeatherShowtimeResponse response = weatherService.getWeatherForShowtime(showtimeId);

        assertNotNull(response);
        assertTrue(response.isOutOfForecastRange());
        assertEquals("Không tìm thấy thông tin suất chiếu.", response.getWarningMessage());
    }

    @Test
    void testGetWeatherForShowtime_CinemaNotFound() {
        showtime.setScreen(null);
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));

        WeatherShowtimeResponse response = weatherService.getWeatherForShowtime(showtimeId);

        assertNotNull(response);
        assertTrue(response.isOutOfForecastRange());
        assertEquals("Không tìm thấy thông tin rạp chiếu.", response.getWarningMessage());
    }

    @Test
    void testGetWeatherForShowtime_CacheHit_Success() {
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(weatherCacheRepository.findById(cinemaId)).thenReturn(Optional.of(cache));

        WeatherShowtimeResponse response = weatherService.getWeatherForShowtime(showtimeId);

        assertNotNull(response);
        assertEquals("Trời quang mây tạnh", response.getCondition());
        assertEquals(28.5, response.getTemperature());
        assertFalse(response.isBadWeather());
        assertFalse(response.isOutOfForecastRange());
    }

    @Test
    void testGetWeatherForShowtime_CacheExpired_ApiSuccess() {
        cache.setLastFetchedAt(LocalDateTime.now(ZONE_VN).minusHours(5));
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(weatherCacheRepository.findById(cinemaId)).thenReturn(Optional.of(cache));

        String mockApiResponse = cache.getLastWeatherData();
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockApiResponse);

        WeatherShowtimeResponse response = weatherService.getWeatherForShowtime(showtimeId);

        assertNotNull(response);
        assertEquals("Trời quang mây tạnh", response.getCondition());
        verify(weatherCacheRepository, times(1)).save(any(CinemaWeatherCache.class));
    }

    @Test
    void testGetWeatherForShowtime_CacheExpired_ApiError_StaleCache() {
        cache.setLastFetchedAt(LocalDateTime.now(ZONE_VN).minusHours(5));
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(weatherCacheRepository.findById(cinemaId)).thenReturn(Optional.of(cache));

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("API Down"));

        WeatherShowtimeResponse response = weatherService.getWeatherForShowtime(showtimeId);

        assertNotNull(response);
        assertEquals("Trời quang mây tạnh", response.getCondition());
    }

    @Test
    void testGetWeatherForShowtime_CacheExpired_ApiError_NoCache() {
        cache.setLastWeatherData(null);
        cache.setLastFetchedAt(null);
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(weatherCacheRepository.findById(cinemaId)).thenReturn(Optional.of(cache));

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("API Down"));

        WeatherShowtimeResponse response = weatherService.getWeatherForShowtime(showtimeId);

        assertNotNull(response);
        assertEquals("Có lỗi xảy ra khi xử lý dữ liệu thời tiết.", response.getWarningMessage());
    }

    @Test
    void testGetWeatherForShowtime_BadWeather_Condition() {
        String badWeatherJson = "{\"forecast\":{\"forecastday\":[{\"date\":\"" + LocalDateTime.now(ZONE_VN).toLocalDate() + "\",\"hour\":[{\"time\":\"" + LocalDateTime.now(ZONE_VN).toLocalDate() + " " + String.format("%02d:00", LocalDateTime.now(ZONE_VN).plusHours(2).getHour()) + "\",\"temp_c\":24.0,\"condition\":{\"text\":\"Mưa dông lớn kèm sấm sét\"}}]}]}}";
        cache.setLastWeatherData(badWeatherJson);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(weatherCacheRepository.findById(cinemaId)).thenReturn(Optional.of(cache));

        WeatherShowtimeResponse response = weatherService.getWeatherForShowtime(showtimeId);

        assertNotNull(response);
        assertTrue(response.isBadWeather());
        assertTrue(response.getWarningMessage().contains("áo mưa"));
    }

    @Test
    void testGetWeatherForShowtime_OutOfForecastRange() {
        // Suất chiếu sau 5 ngày
        showtime.setStartTime(LocalDateTime.now(ZONE_VN).plusDays(5));
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(weatherCacheRepository.findById(cinemaId)).thenReturn(Optional.of(cache));

        WeatherShowtimeResponse response = weatherService.getWeatherForShowtime(showtimeId);

        assertNotNull(response);
        assertTrue(response.isOutOfForecastRange());
        assertTrue(response.getWarningMessage().contains("còn khá xa"));
    }
}
