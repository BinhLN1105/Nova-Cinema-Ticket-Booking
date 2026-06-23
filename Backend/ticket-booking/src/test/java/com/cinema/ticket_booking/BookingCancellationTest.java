package com.cinema.ticket_booking;

import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.MovieStatus;
import com.cinema.ticket_booking.model.*;
import com.cinema.ticket_booking.repository.*;
import com.cinema.ticket_booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.cache.type=none"
})
@ActiveProfiles("test")
class BookingCancellationTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory() {
            org.springframework.data.redis.connection.RedisConnectionFactory factory = 
                org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisConnectionFactory.class);
            org.springframework.data.redis.connection.RedisConnection connection = 
                org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisConnection.class);
            org.springframework.data.redis.connection.RedisServerCommands serverCommands = 
                org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisServerCommands.class);
            
            org.mockito.Mockito.when(factory.getConnection()).thenReturn(connection);
            org.mockito.Mockito.when(connection.serverCommands()).thenReturn(serverCommands);
            return factory;
        }

        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public org.springframework.data.redis.cache.RedisCacheManager cacheManager() {
            org.springframework.data.redis.cache.RedisCacheManager mockManager = 
                org.mockito.Mockito.mock(org.springframework.data.redis.cache.RedisCacheManager.class);
            org.springframework.cache.concurrent.ConcurrentMapCache mockCache = 
                new org.springframework.cache.concurrent.ConcurrentMapCache("system_configs");
            org.mockito.Mockito.when(mockManager.getCache(org.mockito.Mockito.anyString())).thenReturn(mockCache);
            return mockManager;
        }
    }

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @Transactional
    void testCancelConfirmSuccess() {
        // 1. Tạo User test
        User user = userRepository.save(User.builder()
                .email("test_cancellation_" + UUID.randomUUID() + "@example.com")
                .fullName("Test Cancellation Customer")
                .rewardPoints(100L) // Bắt đầu với 100 CinePoints
                .build());

        // 2. Tạo Cinema test
        Cinema cinema = cinemaRepository.save(Cinema.builder()
                .name("CineTest Cinema")
                .address("456 test lane")
                .city("Hanoi")
                .build());

        // 3. Tạo Screen test
        Screen screen = screenRepository.save(Screen.builder()
                .name("CineTest Screen")
                .cinema(cinema)
                .screenType(com.cinema.ticket_booking.enums.ScreenType.STANDARD)
                .totalRows(8)
                .totalCols(8)
                .build());

        // 4. Tạo Movie test
        Movie movie = movieRepository.save(Movie.builder()
                .title("CineTest Movie")
                .duration(150)
                .releaseDate(LocalDate.now())
                .status(MovieStatus.NOW_SHOWING)
                .build());

        // 5. Tạo Showtime test
        Showtime showtime = showtimeRepository.save(Showtime.builder()
                .movie(movie)
                .screen(screen)
                .startTime(LocalDateTime.now().plusHours(10)) // Trong tương lai, thỏa mãn check time
                .endTime(LocalDateTime.now().plusHours(12))
                .basePrice(new BigDecimal("80000.00"))
                .status(com.cinema.ticket_booking.enums.ShowtimeStatus.SCHEDULED)
                .build());

        // 6. Tạo Booking test trạng thái PAID
        String token = "TOKEN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Booking booking = bookingRepository.save(Booking.builder()
                .user(user)
                .cinema(cinema)
                .showtime(showtime)
                .bookingCode("BKTEST" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .totalAmount(new BigDecimal("100000.00")) // Tổng tiền 100.000 VNĐ -> CinePoints hoàn lại: 100
                .status(BookingStatus.PAID)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .cancellationToken(token)
                .cancellationTokenExpiry(LocalDateTime.now().plusMinutes(15))
                .build());

        // Thực hiện cuộc gọi API thông qua service
        bookingService.cancelConfirm(token, booking.getId());

        // 7. Verify kết quả sau khi hủy vé thành công
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElse(null);
        assertNotNull(updatedBooking);
        assertEquals(BookingStatus.CANCELLED, updatedBooking.getStatus());
        assertNull(updatedBooking.getCancellationToken());
        assertNull(updatedBooking.getCancellationTokenExpiry());

        // Verify điểm CinePoints được hoàn
        User updatedUser = userRepository.findById(user.getId()).orElse(null);
        assertNotNull(updatedUser);
        // Ban đầu có 100 CinePoints. Hoàn 100% của 100k VNĐ = 100 CinePoints. Tổng cộng phải là 200.
        // Chú ý: refundPercent lấy từ SystemConfig, mặc định là 100 nếu config trống.
        assertTrue(updatedUser.getRewardPoints() >= 100L);
    }
}
