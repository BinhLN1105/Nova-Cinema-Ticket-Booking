package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.model.Movie;
import com.cinema.ticket_booking.model.Screen;
import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private EmailServiceImpl emailService;

    private User user;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
        user = User.builder()
                .email("user@example.com")
                .fullName("Nguyen Van A")
                .build();
        mimeMessage = mock(MimeMessage.class);
    }

    @Test
    void testSendPasswordResetOtpEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("password-reset-otp"), any(Context.class))).thenReturn("<html>OTP</html>");

        assertDoesNotThrow(() -> emailService.sendPasswordResetOtpEmail(user, "123456"));

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendPasswordResetOtpEmail_Exception() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail server down"));

        // Exception caught internally and logged, should not throw
        assertDoesNotThrow(() -> emailService.sendPasswordResetOtpEmail(user, "123456"));
    }

    @Test
    void testSendBookingConfirmationEmail_Ticket_Success() {
        Cinema cinema = Cinema.builder().name("Nova Cinema Test").build();
        Movie movie = Movie.builder().title("Avengers").build();
        Screen screen = Screen.builder().name("Screen 1").build();
        Showtime showtime = Showtime.builder()
                .movie(movie)
                .screen(screen)
                .startTime(LocalDateTime.now())
                .build();

        Booking booking = Booking.builder()
                .user(user)
                .bookingCode("B001")
                .createdAt(LocalDateTime.now())
                .totalAmount(new BigDecimal("150000"))
                .discountAmount(new BigDecimal("10000"))
                .promotionDiscountAmount(new BigDecimal("5000"))
                .cinema(cinema)
                .showtime(showtime)
                .bookingItems(Collections.emptyList())
                .bookingCombos(Collections.emptyList())
                .qrCode("QR_CODE_DATA")
                .build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("booking-success"), any(Context.class))).thenReturn("<html>Ticket Booking Success</html>");

        assertDoesNotThrow(() -> emailService.sendBookingConfirmationEmail(booking));

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendBookingConfirmationEmail_ConcessionOnly_Success() {
        Booking booking = Booking.builder()
                .user(user)
                .bookingCode("C001")
                .createdAt(LocalDateTime.now())
                .totalAmount(new BigDecimal("50000"))
                .discountAmount(BigDecimal.ZERO)
                .promotionDiscountAmount(BigDecimal.ZERO)
                .cinema(null)
                .showtime(null) // Concession only
                .bookingCombos(Collections.emptyList())
                .build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("concession-success"), any(Context.class))).thenReturn("<html>Concession Success</html>");

        assertDoesNotThrow(() -> emailService.sendBookingConfirmationEmail(booking));

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendBookingConfirmationEmail_Exception() {
        Booking booking = Booking.builder()
                .user(user)
                .build();

        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail error"));

        assertDoesNotThrow(() -> emailService.sendBookingConfirmationEmail(booking));
    }

    @Test
    void testSendCancellationEmail_Success() {
        Booking booking = Booking.builder()
                .user(user)
                .bookingCode("B001")
                .showtime(null)
                .build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("cancellation-success"), any(Context.class))).thenReturn("<html>Cancelled</html>");

        assertDoesNotThrow(() -> emailService.sendCancellationEmail(booking, "User requested"));

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendCancellationEmail_Exception() {
        Booking booking = Booking.builder().user(user).build();
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail error"));

        assertDoesNotThrow(() -> emailService.sendCancellationEmail(booking, "Reason"));
    }

    @Test
    void testSendCancellationRequestEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("cancellation-request"), any(Context.class))).thenReturn("<html>Cancel Request</html>");

        assertDoesNotThrow(() -> emailService.sendCancellationRequestEmail(
                "user@example.com", "Nguyen Van A", "B001", "Avengers", UUID.randomUUID(), "TOKEN123"
        ));

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendCancellationRequestEmail_Exception() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail error"));

        assertDoesNotThrow(() -> emailService.sendCancellationRequestEmail(
                "user@example.com", "Nguyen Van A", "B001", "Avengers", UUID.randomUUID(), "TOKEN123"
        ));
    }
}
