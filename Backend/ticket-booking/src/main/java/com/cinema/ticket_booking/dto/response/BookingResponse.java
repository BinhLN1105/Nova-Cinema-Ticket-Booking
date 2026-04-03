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
    private String movieId;
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
    private BigDecimal subtotal; // Tổng vé + combo (sau khuyến mãi hệ thống, trước voucher)
    private BigDecimal totalOriginalAmount; // Tổng giá niêm yết ban đầu
    private BigDecimal promotionDiscountAmount; // Số tiền được giảm từ hệ thống
    private String appliedPromotionName; // Tên chương trình khuyến mãi hệ thống đã áp dụng
    private String voucherCode;
    private BigDecimal discountAmount; // Số tiền giảm từ voucher
    private BigDecimal totalAmount; // Tổng sau cùng
    private String warningMessage; // Thông báo cảnh báo (ví dụ Voucher hết hạn)

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
        private String movieId;
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
