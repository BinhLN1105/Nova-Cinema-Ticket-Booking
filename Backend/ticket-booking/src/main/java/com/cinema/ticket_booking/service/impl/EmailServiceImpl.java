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
    public void sendPasswordResetOtpEmail(User user, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("[Nova Ticket] Mã xác thực khôi phục mật khẩu: " + otp);

            Context context = new Context();
            context.setVariable("customerName", user.getFullName());
            context.setVariable("otp", otp);

            String htmlContent = templateEngine.process("password-reset-otp", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to send OTP email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @Async("asyncExecutor")
    public void sendBookingConfirmationEmail(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("[Nova Ticket] Đặt vé thành công - Mã vé: " + booking.getBookingCode());

            Context context = new Context();
            context.setVariable("customerName", booking.getUser().getFullName());
            context.setVariable("bookingCode", booking.getBookingCode());
            context.setVariable("movieTitle", booking.getShowtime().getMovie().getTitle());
            context.setVariable("cinemaName", booking.getShowtime().getScreen().getCinema().getName());
            context.setVariable("screenName", booking.getShowtime().getScreen().getName());
            
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            context.setVariable("showTime", booking.getShowtime().getStartTime().format(formatter));
            
            String totalFormatted = booking.getTotalAmount() != null 
                ? String.format("%,.0f", booking.getTotalAmount()) : "0";
            context.setVariable("totalAmount", totalFormatted);

            String htmlContent = templateEngine.process("booking-success", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to send booking confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @Async("asyncExecutor")
    public void sendCancellationEmail(Booking booking, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("[Nova Ticket] Thông báo hủy vé thành công - Mã: " + booking.getBookingCode());

            Context context = new Context();
            context.setVariable("customerName", booking.getUser().getFullName());
            context.setVariable("bookingCode", booking.getBookingCode());
            context.setVariable("movieTitle", booking.getShowtime().getMovie().getTitle());
            context.setVariable("reason", reason != null ? reason : "Hệ thống tự động hủy theo yêu cầu.");

            String htmlContent = templateEngine.process("cancellation-success", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to send cancellation email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
