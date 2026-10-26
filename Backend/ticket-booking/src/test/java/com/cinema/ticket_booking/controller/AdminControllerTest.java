package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.PaymentMethod;
import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.model.*;
import com.cinema.ticket_booking.repository.*;
import com.cinema.ticket_booking.service.AnalyticsService;
import com.cinema.ticket_booking.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private StaffProfileRepository staffProfileRepository;

    @Mock
    private CinemaRepository cinemaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminController adminController;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Admin Test")
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .build();

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(User.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return adminUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    void testUpdateUserRole_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        User targetUser = User.builder().id(userId).email("user@test.com").role(UserRole.CUSTOMER).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"STAFF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userRepository).save(argThat(u -> u.getRole() == UserRole.STAFF));
    }

    @Test
    void testGetAllBookings_ConcessionOnlyAndNormal() throws Exception {
        Cinema cinema = Cinema.builder().id(UUID.randomUUID()).name("Cinema Test").build();
        Screen screen = Screen.builder().id(UUID.randomUUID()).name("Screen 1").cinema(cinema).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Movie Test").posterUrl("poster.jpg").build();
        Showtime showtime = Showtime.builder()
                .id(UUID.randomUUID())
                .screen(screen)
                .movie(movie)
                .startTime(LocalDateTime.now())
                .build();

        Booking normalBooking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BK_NORMAL")
                .showtime(showtime)
                .totalAmount(BigDecimal.valueOf(100000))
                .status(BookingStatus.PAID)
                .paymentMethod(PaymentMethod.VNPAY)
                .createdAt(LocalDateTime.now())
                .build();

        Booking concessionBooking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BK_CONCESSION")
                .cinema(cinema)
                .showtime(null)
                .totalAmount(BigDecimal.valueOf(50000))
                .status(BookingStatus.PAID)
                .paymentMethod(PaymentMethod.WALLET)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Booking> page = new PageImpl<>(List.of(normalBooking, concessionBooking), PageRequest.of(0, 10), 2);
        when(bookingRepository.searchBookings(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].bookingCode").value("BK_NORMAL"))
                .andExpect(jsonPath("$.data.content[0].movieTitle").value("Movie Test"))
                .andExpect(jsonPath("$.data.content[1].bookingCode").value("BK_CONCESSION"))
                .andExpect(jsonPath("$.data.content[1].movieTitle").value("🍿 Hóa đơn F&B"));
    }
}
