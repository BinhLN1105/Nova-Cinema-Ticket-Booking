package com.cinema.ticket_booking.dto.request;

import com.cinema.ticket_booking.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BookingRequest {

    // Nullable cho đơn lẻ bắp nước (POS)
    private String showtimeId;

    // Danh sách showtimeSeatId đã chọn (Có thể trống cho đơn lẻ bắp nước)
    private List<String> showtimeSeatIds;

    // Combo thêm vào (có thể rỗng)
    private List<ComboItem> combos;

    // Mã voucher (null nếu không dùng)
    private String voucherCode;

    // Phương thức thanh toán (CASH, VNPAY...)
    private PaymentMethod paymentMethod;

    @Data
    public static class ComboItem {
        private String comboId;
        private Integer quantity;
    }
}
