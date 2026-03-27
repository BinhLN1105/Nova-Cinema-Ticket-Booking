package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String bookingCode;
    private BookingStatus status;

    // Thông tin suất chiếu
    private String showtimeId;
    private String movieTitle;
    private String moviePosterUrl;
    private LocalDateTime startTime;
    private String cinemaName;
    private String cinemaAddress;
    private String screenName;
    private String screenType;

    // Ghế đã đặt
    private List<SeatItem> seats;

    // Combo
    private List<ComboItem> combos;

    // Thanh toán
    private BigDecimal subtotal; // Tổng trước giảm giá
    private String voucherCode;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount; // Tổng sau giảm giá

    // QR Code (chỉ có khi status = PAID)
    private String qrCode;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt; // Hết hạn thanh toán

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String showtimeSeatId;
        private Character rowLabel;
        private Integer colNumber;
        private String seatType;
        private BigDecimal price;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String comboId;
        private String comboName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    // Tóm tắt ngắn dùng cho danh sách lịch sử đặt vé
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String bookingCode;
        private String movieTitle;
        private String moviePosterUrl;
        private LocalDateTime startTime;
        private String cinemaName;
        private String screenName;
        private String screenType;
        private String seats;
        private BigDecimal totalAmount;
        private BookingStatus status;
        private LocalDateTime createdAt;
    }
}
