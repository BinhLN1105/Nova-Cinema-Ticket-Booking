package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.EmailService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
            String subject = booking.getShowtime() != null 
                ? "[Nova Ticket] Đặt vé thành công - Mã vé: " + booking.getBookingCode()
                : "[Nova Ticket] Đặt bắp nước thành công - Mã đơn: " + booking.getBookingCode();
            helper.setSubject(subject);

            boolean isConcessionOnly = booking.getShowtime() == null;
            String templateName = isConcessionOnly ? "concession-success" : "booking-success";

            Context context = new Context();
            context.setVariable("userName", booking.getUser().getFullName());
            context.setVariable("bookingCode", booking.getBookingCode());
            context.setVariable("orderDate", booking.getCreatedAt());
            context.setVariable("totalAmount", booking.getTotalAmount());
            context.setVariable("discountAmount", booking.getDiscountAmount().add(booking.getPromotionDiscountAmount()));

            // Cinema info
            String cinemaName = "Nova Cinema";
            if (booking.getCinema() != null) {
                cinemaName = booking.getCinema().getName();
            } else if (!isConcessionOnly) {
                cinemaName = booking.getShowtime().getScreen().getCinema().getName();
            }
            context.setVariable("cinemaName", cinemaName);

            if (!isConcessionOnly) {
                context.setVariable("movieTitle", booking.getShowtime().getMovie().getTitle());
                context.setVariable("showtime", booking.getShowtime().getStartTime());
                context.setVariable("screenName", booking.getShowtime().getScreen().getName());

                List<String> seatLabels = booking.getBookingItems().stream()
                        .map(item -> item.getShowtimeSeat().getSeat().getSeatLabel())
                        .collect(Collectors.toList());
                context.setVariable("seats", String.join(", ", seatLabels));
                context.setVariable("qrCode", booking.getQrCode());
            }

            // Combo items for both types
            List<Map<String, Object>> comboList = booking.getBookingCombos().stream()
                    .map(bc -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", bc.getCombo().getName());
                        map.put("quantity", bc.getQuantity());
                        map.put("totalPrice", bc.getUnitPrice().multiply(BigDecimal.valueOf(bc.getQuantity())));
                        return map;
                    }).collect(Collectors.toList());
            context.setVariable("combos", comboList);

            String htmlContent = templateEngine.process(templateName, context);
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
            helper.setSubject("[Nova Ticket] Thông báo hủy đơn hàng - Mã: " + booking.getBookingCode());

            Context context = new Context();
            context.setVariable("customerName", booking.getUser().getFullName());
            context.setVariable("bookingCode", booking.getBookingCode());
            
            String movieTitle = booking.getShowtime() != null 
                ? booking.getShowtime().getMovie().getTitle() 
                : "Bắp nước & Combo";
            context.setVariable("movieTitle", movieTitle);
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
