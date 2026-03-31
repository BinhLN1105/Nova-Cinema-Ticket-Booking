package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Async("asyncExecutor")
    public void sendCancellationConfirmEmail(Booking booking, String token) {
        String confirmUrl = frontendUrl + "/booking/cancel-confirm?token="
                + token + "&bookingId=" + booking.getId().toString();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("Xác nhận hủy vé xem phim - Mã đơn: " + booking.getBookingCode());

            Context context = new Context();
            context.setVariable("customerName", booking.getUser().getFullName());
            context.setVariable("bookingCode", booking.getBookingCode());
            context.setVariable("cinemaName", booking.getShowtime().getScreen().getCinema().getName());
            context.setVariable("confirmUrl", confirmUrl);

            String htmlContent = templateEngine.process("cancellation-confirm", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to send cancellation email: " + e.getMessage());
            // Log full stack trace but don't rethrow to avoid crashing caller
            e.printStackTrace();
        }
    }

    @Override
    @Async("asyncExecutor")
    public void sendPasswordResetEmail(User user, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("Khôi phục mật khẩu - Nova Ticket");

            Context context = new Context();
            context.setVariable("customerName", user.getFullName());
            context.setVariable("resetUrl", resetUrl);

            String htmlContent = templateEngine.process("password-reset", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to send password reset email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
