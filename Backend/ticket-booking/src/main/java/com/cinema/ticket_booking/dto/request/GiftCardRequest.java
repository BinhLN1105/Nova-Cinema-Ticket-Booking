package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

public class GiftCardRequest {

    @Data
    public static class Buy {
        @NotNull(message = "Giá trị thẻ không được để trống")
        @DecimalMin(value = "50000", message = "Mệnh giá tối thiểu là 50000 VNĐ")
        private BigDecimal price;

        @NotBlank(message = "returnUrlBase không được để trống")
        private String returnUrlBase;
    }

    @Data
    public static class Redeem {
        @NotBlank(message = "Mã thẻ không được để trống")
        private String code;
    }
}
