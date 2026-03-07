package com.cinema.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BookingRequest {

    @NotNull(message = "Suất chiếu không được để trống")
    private String showtimeId;

    // Danh sách showtimeSeatId đã chọn
    @NotEmpty(message = "Phải chọn ít nhất 1 ghế")
    private List<String> showtimeSeatIds;

    // Combo thêm vào (có thể rỗng)
    private List<ComboItem> combos;

    // Mã voucher (null nếu không dùng)
    private String voucherCode;

    @Data
    public static class ComboItem {
        private String comboId;
        private Integer quantity;
    }
}
